//! Universal Dynamic Server Endpoint.
//!
//! Supports flexible addresses without any hardcoded IPs:
//! - "130.162.254.32"
//! - "130.162.254.32:8443"
//! - "cloud.neongram.space"
//! - "cloud.neongram.space:443/api"
//! - "https://node.domain.com:8443/ws"

use crate::error::NetworkError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ProtocolScheme {
    Tcp,
    Udp,
    Http,
    Https,
    Ws,
    Wss,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ServerEndpoint {
    pub scheme: ProtocolScheme,
    pub host: String,
    pub port: u16,
    pub path: String,
}

impl ServerEndpoint {
    pub fn new(host: &str, port: u16) -> Self {
        Self {
            scheme: ProtocolScheme::Https,
            host: host.to_string(),
            port,
            path: String::new(),
        }
    }

    /// Parses arbitrary user or push endpoint string into structured endpoint.
    pub fn parse(raw: &str) -> Result<Self, NetworkError> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return Err(NetworkError::InvalidEndpoint("Empty endpoint string".into()));
        }

        let (scheme, rest) = if let Some(stripped) = trimmed.strip_prefix("https://") {
            (ProtocolScheme::Https, stripped)
        } else if let Some(stripped) = trimmed.strip_prefix("http://") {
            (ProtocolScheme::Http, stripped)
        } else if let Some(stripped) = trimmed.strip_prefix("wss://") {
            (ProtocolScheme::Wss, stripped)
        } else if let Some(stripped) = trimmed.strip_prefix("ws://") {
            (ProtocolScheme::Ws, stripped)
        } else if let Some(stripped) = trimmed.strip_prefix("tcp://") {
            (ProtocolScheme::Tcp, stripped)
        } else if let Some(stripped) = trimmed.strip_prefix("udp://") {
            (ProtocolScheme::Udp, stripped)
        } else {
            (ProtocolScheme::Https, trimmed)
        };

        // Split host[:port] and /path
        let (host_port, path) = match rest.find('/') {
            Some(idx) => (&rest[..idx], rest[idx..].to_string()),
            None => (rest, String::new()),
        };

        let (host, port) = if let Some(colon_idx) = host_port.rfind(':') {
            let host_part = &host_port[..colon_idx];
            let port_part = &host_port[colon_idx + 1..];
            let parsed_port = port_part
                .parse::<u16>()
                .map_err(|_| NetworkError::InvalidEndpoint(format!("Invalid port: {}", port_part)))?;
            (host_part.to_string(), parsed_port)
        } else {
            let default_port = match scheme {
                ProtocolScheme::Http | ProtocolScheme::Ws => 80,
                ProtocolScheme::Https | ProtocolScheme::Wss => 443,
                ProtocolScheme::Tcp => 8080,
                ProtocolScheme::Udp => 51820,
            };
            (host_port.to_string(), default_port)
        };

        if host.is_empty() {
            return Err(NetworkError::InvalidEndpoint("Host cannot be empty".into()));
        }

        Ok(Self {
            scheme,
            host,
            port,
            path,
        })
    }

    pub fn to_address_string(&self) -> String {
        format!("{}:{}", self.host, self.port)
    }

    pub fn full_url(&self) -> String {
        let scheme_str = match self.scheme {
            ProtocolScheme::Http => "http",
            ProtocolScheme::Https => "https",
            ProtocolScheme::Ws => "ws",
            ProtocolScheme::Wss => "wss",
            ProtocolScheme::Tcp => "tcp",
            ProtocolScheme::Udp => "udp",
        };
        format!("{}://{}:{}{}", scheme_str, self.host, self.port, self.path)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_ip_only() {
        let ep = ServerEndpoint::parse("130.162.254.32").unwrap();
        assert_eq!(ep.host, "130.162.254.32");
        assert_eq!(ep.port, 443);
        assert_eq!(ep.path, "");
    }

    #[test]
    fn test_parse_ip_and_port() {
        let ep = ServerEndpoint::parse("130.162.254.32:8443").unwrap();
        assert_eq!(ep.host, "130.162.254.32");
        assert_eq!(ep.port, 8443);
    }

    #[test]
    fn test_parse_domain_port_and_path() {
        let ep = ServerEndpoint::parse("cloud.neongram.space:443/api/v1/sync").unwrap();
        assert_eq!(ep.host, "cloud.neongram.space");
        assert_eq!(ep.port, 443);
        assert_eq!(ep.path, "/api/v1/sync");
    }

    #[test]
    fn test_parse_full_url() {
        let ep = ServerEndpoint::parse("wss://relay.anongram.net:9000/stream").unwrap();
        assert_eq!(ep.scheme, ProtocolScheme::Wss);
        assert_eq!(ep.host, "relay.anongram.net");
        assert_eq!(ep.port, 9000);
        assert_eq!(ep.path, "/stream");
    }
}
