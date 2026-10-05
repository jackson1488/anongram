export interface EncryptedEnvelope {
  type: "envelope";
  recipientId: string;
  senderId: string;
  shieldLevel: "gold" | "blue" | "pq_hybrid";
  payloadBase64: string;
  timestamp: number;
}

export interface IRelayRouter {
  registerClient(publicId: string, socket: any): void;
  unregisterClient(publicId: string): void;
  routeEnvelope(envelope: EncryptedEnvelope): boolean;
}
