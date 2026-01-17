package com.chat.server.network;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer {

    private static final int PORT = 8887; // Port riêng cho WebSocket
    private static WebSocketServer instance;

    public WebSocketServer() {
        super(new InetSocketAddress(PORT));
        instance = this;
    }

    // --- CÁC HÀM OVERRIDE CỦA THƯ VIỆN ---
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[WebSocket] New monitor connected: " + conn.getRemoteSocketAddress());
        conn.send("--- Connected to Chat Server Log Stream ---");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[WebSocket] Monitor disconnected: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Không xử lý tin nhắn từ Web gửi lên (chỉ 1 chiều Server -> Web)
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocket] Log Server started on port " + PORT);
    }

    // --- HÀM STATIC ĐỂ GỬI LOG TỪ BẤT KỲ ĐÂU ---
    /**
     * Gửi log tới tất cả các trình duyệt đang mở trang theo dõi.
     * @param message Nội dung log
     */
    public static void broadcastLog(String message) {
        if (instance != null) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logMsg = String.format("[%s] %s", time, message);
            
            // Gửi tới tất cả client đang kết nối
            instance.broadcast(logMsg); 
        }
    }
}