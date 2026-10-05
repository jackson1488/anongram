import Fastify from "fastify";
import websocket from "@fastify/websocket";
import { SqlitePassportStorage } from "./infrastructure/storage/sqlite_passport_storage.js";
import { BlindRelayRouter } from "./domain/services/blind_relay_router.js";
import type { PassportRecord } from "./core/interfaces/i_passport_storage.js";
import type { EncryptedEnvelope } from "./core/interfaces/i_relay_router.js";

const app = Fastify({ logger: false });
await app.register(websocket);

const passportStorage = new SqlitePassportStorage(process.env.DB_PATH ?? "anongram_server.db");
const relayRouter = new BlindRelayRouter();

// Health check
app.get("/health", async () => ({
  status: "ok",
  timestamp: Date.now(),
  service: "anongram-blind-relay",
}));

// Publish or update passport blob
app.post<{ Body: PassportRecord }>("/api/v1/passport", async (req, reply) => {
  const { publicId, version, blobBase64 } = req.body;
  if (!publicId || typeof version !== "number" || !blobBase64) {
    return reply.status(400).send({ error: "Invalid passport format" });
  }

  const saved = await passportStorage.savePassport({
    publicId,
    version,
    blobBase64,
    updatedAt: Math.floor(Date.now() / 1000),
  });

  if (!saved) {
    return reply.status(409).send({ error: "Version rollback or downgrade rejected" });
  }

  return { status: "saved", publicId, version };
});

// Retrieve public passport blob
app.get<{ Params: { publicId: string } }>("/api/v1/passport/:publicId", async (req, reply) => {
  const passport = await passportStorage.getPassport(req.params.publicId);
  if (!passport) {
    return reply.status(404).send({ error: "Passport not found" });
  }
  return passport;
});

// WebSocket Blind Relay
app.register(async function (fastify) {
  fastify.get("/ws", { websocket: true }, (socket, req) => {
    let authenticatedId: string | null = null;

    socket.on("message", (raw: Buffer) => {
      try {
        const msg = JSON.parse(raw.toString("utf8"));

        if (msg.type === "auth" && typeof msg.publicId === "string") {
          authenticatedId = msg.publicId;
          relayRouter.registerClient(authenticatedId, socket);
          socket.send(JSON.stringify({ type: "auth_ack", publicId: authenticatedId }));
          return;
        }

        if (msg.type === "envelope") {
          const envelope: EncryptedEnvelope = {
            type: "envelope",
            recipientId: msg.recipientId,
            senderId: authenticatedId ?? msg.senderId,
            shieldLevel: msg.shieldLevel ?? "pq_hybrid",
            payloadBase64: msg.payloadBase64,
            timestamp: Math.floor(Date.now() / 1000),
          };
          const delivered = relayRouter.routeEnvelope(envelope);
          socket.send(JSON.stringify({ type: "ack", delivered }));
        }
      } catch {
        socket.send(JSON.stringify({ error: "Malformed payload" }));
      }
    });

    socket.on("close", () => {
      if (authenticatedId) {
        relayRouter.unregisterClient(authenticatedId);
      }
    });
  });
});

const port = Number(process.env.PORT ?? 8080);
await app.listen({ port, host: "0.0.0.0" });
