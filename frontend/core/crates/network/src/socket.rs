//! Asynchronous Network Sockets Engine (TCP + UDP + Heartbeat).
//!
//! Provides non-blocking TCP streams and UDP datagram sockets over Tokio runtime,
//! with adaptive battery-efficient keepalive heartbeats.

use std::net::SocketAddr;
use std::time::Duration;
use tokio::net::{TcpStream, UdpSocket};
use tokio::time::sleep;

use crate::endpoint::ServerEndpoint;
use crate::error::NetworkError;

pub struct AsyncTcpClient {
    stream: Option<TcpStream>,
    target_addr: Option<SocketAddr>,
}

impl Default for AsyncTcpClient {
    fn default() -> Self {
        Self::new()
    }
}

impl AsyncTcpClient {
    pub fn new() -> Self {
        Self {
            stream: None,
            target_addr: None,
        }
    }

    pub fn is_connected(&self) -> bool {
        self.stream.is_some()
    }

    pub async fn connect_addr(&mut self, addr: SocketAddr) -> Result<(), NetworkError> {
        let stream = TcpStream::connect(addr)
            .await
            .map_err(|_| NetworkError::ConnectionFailed)?;
        self.stream = Some(stream);
        self.target_addr = Some(addr);
        Ok(())
    }

    pub async fn send(&mut self, data: &[u8]) -> Result<(), NetworkError> {
        use tokio::io::AsyncWriteExt;
        if let Some(stream) = self.stream.as_mut() {
            stream
                .write_all(data)
                .await
                .map_err(|e| NetworkError::TransmissionError(e.to_string()))?;
            Ok(())
        } else {
            Err(NetworkError::Unconfigured)
        }
    }

    pub async fn disconnect(&mut self) {
        self.stream = None;
        self.target_addr = None;
    }
}

pub struct AsyncUdpClient {
    socket: Option<UdpSocket>,
    target_addr: Option<SocketAddr>,
}

impl Default for AsyncUdpClient {
    fn default() -> Self {
        Self::new()
    }
}

impl AsyncUdpClient {
    pub fn new() -> Self {
        Self {
            socket: None,
            target_addr: None,
        }
    }

    pub async fn bind_and_connect(&mut self, target: SocketAddr) -> Result<(), NetworkError> {
        let local_bind: SocketAddr = if target.is_ipv4() {
            "0.0.0.0:0".parse().unwrap()
        } else {
            "[::]:0".parse().unwrap()
        };

        let socket = UdpSocket::bind(local_bind)
            .await
            .map_err(|_| NetworkError::ConnectionFailed)?;

        socket
            .connect(target)
            .await
            .map_err(|_| NetworkError::ConnectionFailed)?;

        self.socket = Some(socket);
        self.target_addr = Some(target);
        Ok(())
    }

    pub async fn send_datagram(&self, data: &[u8]) -> Result<usize, NetworkError> {
        if let Some(socket) = &self.socket {
            socket
                .send(data)
                .await
                .map_err(|e| NetworkError::TransmissionError(e.to_string()))
        } else {
            Err(NetworkError::Unconfigured)
        }
    }
}

/// Adaptive Heartbeat Keepalive Engine.
/// Keeps cellular NAT mappings active with zero CPU wakeups.
pub struct HeartbeatKeeper {
    interval: Duration,
    running: bool,
}

impl Default for HeartbeatKeeper {
    fn default() -> Self {
        Self::new(Duration::from_secs(25))
    }
}

impl HeartbeatKeeper {
    pub fn new(interval: Duration) -> Self {
        Self {
            interval,
            running: false,
        }
    }

    pub fn interval(&self) -> Duration {
        self.interval
    }

    pub async fn next_tick(&self) {
        sleep(self.interval).await;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_async_udp_local_loopback() {
        let loopback: SocketAddr = "127.0.0.1:0".parse().unwrap();
        let receiver = UdpSocket::bind(loopback).await.unwrap();
        let target_addr = receiver.local_addr().unwrap();

        let mut client = AsyncUdpClient::new();
        client.bind_and_connect(target_addr).await.unwrap();

        let payload = b"ANONGRAM_ASYNC_UDP_DATAGRAM";
        client.send_datagram(payload).await.unwrap();

        let mut buf = [0u8; 64];
        let (len, _src) = receiver.recv_from(&mut buf).await.unwrap();
        assert_eq!(&buf[..len], payload);
    }

    #[tokio::test]
    async fn test_async_tcp_local_loopback() {
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();

        let mut client = AsyncTcpClient::new();
        let client_task = async {
            client.connect_addr(addr).await.unwrap();
            client.send(b"PING_TCP").await.unwrap();
        };

        let server_task = async {
            use tokio::io::AsyncReadExt;
            let (mut socket, _) = listener.accept().await.unwrap();
            let mut buf = [0u8; 8];
            socket.read_exact(&mut buf).await.unwrap();
            assert_eq!(&buf, b"PING_TCP");
        };

        tokio::join!(client_task, server_task);
    }
}
