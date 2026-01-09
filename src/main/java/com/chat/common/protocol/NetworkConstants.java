package com.chat.common.protocol;

/**
 * Các hằng số cấu hình mạng dùng chung.
 */
public class NetworkConstants {
    // TCP Ports
    public static final int TCP_PORT = 8888;
    public static final int TCP_SSL_PORT = 8889;

    // UDP Ports
    public static final int UDP_DISCOVERY_PORT = 9999;
    public static final int UDP_BUZZ_PORT = 9998;

    // Multicast
    public static final String MULTICAST_ADDRESS = "230.0.0.1";
    public static final int MULTICAST_PORT = 9997;

    // WebSocket
    public static final int WEBSOCKET_PORT = 8080;

    // gRPC
    public static final int GRPC_PORT = 50051;

    // Discovery Messages
    public static final String DISCOVERY_REQUEST = "WHERE_IS_SERVER?";
    public static final String DISCOVERY_RESPONSE = "I_AM_SERVER";

    private NetworkConstants() {
        // Prevent instantiation
    }
}
