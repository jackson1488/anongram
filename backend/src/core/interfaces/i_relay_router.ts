export interface EncryptedEnvelope {
  type: "envelope";
  recipientId: string;
  senderId: string;
  shieldLevel: "gold" | "blue" | "pq_hybrid";
  payloadBase64: string;
  timestamp: number;
}

export interface CallSignalMessage {
  type: "call_signal";
  callId: string;
  senderId: string;
  recipientId: string;
  signalType: "invite" | "offer" | "answer" | "candidate" | "upgrade_group" | "hangup";
  payloadJson: string;
  timestamp: number;
}

export interface IRelayRouter {
  registerClient(publicId: string, socket: any): void;
  unregisterClient(publicId: string): void;
  routeEnvelope(envelope: EncryptedEnvelope): boolean;
  routeCallSignal(signal: CallSignalMessage): boolean;
}

