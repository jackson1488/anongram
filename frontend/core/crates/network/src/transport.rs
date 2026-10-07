//! Dual-Channel Transport Engine (TCP + UDP) with Dynamic Endpoint updates.

use crate::endpoint::ServerEndpoint;
use crate::error::NetworkError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TransportChannel {
    TcpStream,
    UdpDatagram,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NetworkPacket {
    pub channel: TransportChannel,
    pub payload: Vec<u8>,
}

pub struct NetworkManager {
    active_endpoint: Option<ServerEndpoint>,
    active_channel: TransportChannel,
    bytes_sent: u64,
    bytes_received: u64,
}

impl Default for NetworkManager {
    fn default() -> Self {
        Self::new()
    }
}

impl NetworkManager {
    pub fn new() -> Self {
        Self {
            active_endpoint: None,
            active_channel: TransportChannel::UdpDatagram,
            bytes_sent: 0,
            bytes_received: 0,
        }
    }

    pub fn endpoint(&self) -> Option<&ServerEndpoint> {
        self.active_endpoint.as_ref()
    }

    /// Sets or rotates server endpoint dynamically (e.g. from Push command or config).
    pub fn set_endpoint(&mut self, endpoint: ServerEndpoint) {
        self.active_endpoint = Some(endpoint);
    }

    /// Rotates endpoint from arbitrary raw string without app restart.
    pub fn rotate_endpoint_from_str(&mut self, raw: &str) -> Result<(), NetworkError> {
        let ep = ServerEndpoint::parse(raw)?;
        self.set_endpoint(ep);
        Ok(())
    }

    pub fn active_channel(&self) -> TransportChannel {
        self.active_channel
    }

    pub fn set_channel(&mut self, channel: TransportChannel) {
        self.active_channel = channel;
    }

    /// Automatically switches to TCP if UDP is throttled by DPI.
    pub fn failover_to_tcp(&mut self) {
        self.active_channel = TransportChannel::TcpStream;
    }

    /// Prepares packet for transmission over the active channel.
    pub fn send_packet(&mut self, payload: &[u8]) -> Result<NetworkPacket, NetworkError> {
        if self.active_endpoint.is_none() {
            return Err(NetworkError::Unconfigured);
        }

        self.bytes_sent += payload.len() as u64;
        Ok(NetworkPacket {
            channel: self.active_channel,
            payload: payload.to_vec(),
        })
    }

    /// Processes received incoming packet.
    pub fn receive_packet(&mut self, packet: NetworkPacket) -> Result<Vec<u8>, NetworkError> {
        if self.active_endpoint.is_none() {
            return Err(NetworkError::Unconfigured);
        }

        self.bytes_received += packet.payload.len() as u64;
        Ok(packet.payload)
    }

    pub fn stats(&self) -> (u64, u64) {
        (self.bytes_sent, self.bytes_received)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_network_manager_lifecycle_and_rotation() {
        let mut net = NetworkManager::new();
        assert_eq!(net.send_packet(b"ping").err(), Some(NetworkError::Unconfigured));

        // Configure dynamic IP
        net.rotate_endpoint_from_str("130.162.254.32:443").unwrap();
        assert_eq!(net.endpoint().unwrap().host, "130.162.254.32");

        // Send over UDP
        let pkt = net.send_packet(b"hello udp").unwrap();
        assert_eq!(pkt.channel, TransportChannel::UdpDatagram);

        // DPI blocks UDP -> failover to TCP
        net.failover_to_tcp();
        assert_eq!(net.active_channel(), TransportChannel::TcpStream);

        // Send over TCP
        let pkt_tcp = net.send_packet(b"hello tcp").unwrap();
        assert_eq!(pkt_tcp.channel, TransportChannel::TcpStream);

        // Rotate to domain with API path
        net.rotate_endpoint_from_str("cloud.neongram.space/api/v1").unwrap();
        assert_eq!(net.endpoint().unwrap().host, "cloud.neongram.space");
        assert_eq!(net.endpoint().unwrap().path, "/api/v1");
    }
}
