import Fastify from "fastify";

const app = Fastify({ logger: false });

app.get("/health", async () => ({ status: "ok" }));

const port = Number(process.env.PORT ?? 8080);
await app.listen({ port, host: "0.0.0.0" });
