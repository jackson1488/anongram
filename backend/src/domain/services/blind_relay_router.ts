import type { EncryptedEnvelope, IRelayRouter } from "../core/interfaces/i_relay_router.js";

interface ActiveSession {
  publicId: string;
  socket: {
    send: (data: string) => void;
  };
}

export class BlindRelayRouter implements IRelayRouter {
  private activeClients = new Map<string, ActiveSession>();
  private offlineBuffer = new Map<string, EncryptedEnvelope[]>();
  private readonly maxOfflinePerClient = 100;

  public registerClient(publicId: string, socket: { send: (data: string) => void }): void {
    this.activeClients.set(publicId, { publicId, socket });

    // Flush any pending offline envelopes
    const pending = this.offlineBuffer.get(publicId);
    if (pending && pending.length > 0) {
      for (const env of pending) {
        socket.send(JSON.stringify(env));
      }
      this.offlineBuffer.delete(publicId);
    }
  }

  public unregisterClient(publicId: string): void {
    this.activeClients.delete(publicId);
  }

  public routeEnvelope(envelope: EncryptedEnvelope): boolean {
    const client = this.activeClients.get(envelope.recipientId);
    if (client) {
      try {
        client.socket.send(JSON.stringify(envelope));
        return true;
      } catch {
        this.activeClients.delete(envelope.recipientId);
      }
    }

    // Buffer for offline client
    let queue = this.offlineBuffer.get(envelope.recipientId);
    if (!queue) {
      queue = [];
      this.offlineBuffer.set(envelope.recipientId, queue);
    }

    if (queue.length >= this.maxOfflinePerClient) {
      queue.shift(); // Drop oldest message if queue is full
    }
    queue.push(envelope);
    return false;
  }
}
