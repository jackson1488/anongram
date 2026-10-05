# AnonGram Protocol Specification v1 (Zero-Knowledge Blind Relay)

## 1. Principles
- **Blind Relay**: The server NEVER sees plaintext, private keys, or OTP seeds/pads.
- **Client Identity**: Public ID = Hex SHA-256 of the hybrid master verifying key (`Ed25519` + `ML-DSA-87`).
- **Cryptographic Proof of Ownership**: Registering or updating keys requires a valid cryptographic signature.
- **Stateless Envelope Routing**: Messages are forwarded purely by recipient ID without inspection.

---

## 2. Data Types

### 2.1 Prekey Bundle (`POST /api/v1/prekeys`)
```json
{
  "public_id": "8f3ab...921",
  "master_vk": {
    "ed25519_hex": "...",
    "mldsa_hex": "..."
  },
  "prekey": {
    "x25519_hex": "...",
    "mlkem_hex": "..."
  },
  "signature": {
    "ed25519_hex": "...",
    "mldsa_hex": "..."
  }
}
```

### 2.2 Encrypted Envelope (WebSocket Frame)
```json
{
  "type": "envelope",
  "recipient_id": "8f3ab...921",
  "sender_id": "7c12d...01a",
  "shield_level": "gold" | "blue" | "pq_hybrid",
  "payload_base64": "...",
  "timestamp": 1700000000
}
```

---

## 3. Server Endpoints
- `GET /health` — health check status
- `POST /api/v1/passport` — publish/update signed Passport Blob
- `GET /api/v1/passport/:public_id` — retrieve public Passport Blob
- `WS /ws` — bidirectional blind message relay and offline queues
