package com.chat.server;

import com.chat.bot.CensorBotServer;
import com.chat.server.core.ServerHandler;
import com.chat.server.network.HttpFileServer;
import com.chat.server.network.MulticastAdminServer;
import com.chat.server.network.UdpDiscoveryServer;
import com.chat.server.network.WebSocketServer;
import com.chat.common.crypto.SSLUtil;
import com.chat.common.protocol.NetworkConstants;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int MAX_THREADS = 100;

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        System.out.println(">>> ĐANG KHỞI ĐỘNG HỆ THỐNG CHAT SERVER ĐA DỊCH VỤ <<<");

        // --- 1. KHỞI CHẠY CENSOR BOT (gRPC) ---
        // Bot chạy ở Port 50051 để lọc từ bậy
        new Thread(() -> {
            try {
                System.out.println("[Service] Starting Censor Bot (Port 50051)...");
                CensorBotServer.main(new String[]{});
            } catch (Exception e) {
                System.err.println("⚠ Không thể khởi động Bot: " + e.getMessage());
            }
        }).start();

        // --- 2. KHỞI CHẠY HTTP FILE SERVER ---
        // Server chạy ở Port 8080 để upload/download ảnh
        new Thread(() -> {
            System.out.println("[Service] Starting HTTP File Server (Port 8080)...");
            new HttpFileServer().start();
        }).start();

        // --- 3. KHỞI CHẠY WEBSOCKET LOG SERVER ---
        // Server chạy ở Port 8887 để đẩy log lên Web (Realtime)
        new Thread(() -> {
            System.out.println("[Service] Starting WebSocket Log Server (Port 8887)...");
            new WebSocketServer().run();
        }).start();

        // --- 4. KHỞI CHẠY UDP DISCOVERY ---
        // Giúp Client quét mạng LAN để tìm thấy Server
        // Lưu ý: Dùng new Thread(...) vì UdpDiscoveryServer implements Runnable
        new Thread(() -> {
            System.out.println("[Service] Starting UDP Discovery Server (Port 8889)...");
            new UdpDiscoveryServer().start();
        }).start();

        // --- 4. KHỞI CHẠY MULTICAST ADMIN (Tùy chọn) ---
        new Thread(() -> {
            System.out.println("[Service] Starting Multicast Admin Server (Port 8890)...");
            new MulticastAdminServer().start();
        }).start();

        // --- 6. KHỞI CHẠY SSL/TLS CHAT SERVER (Port 8889) ---
        // Server CHỈ chạy SSL/TLS (mã hóa) - Đã bỏ TCP thường để đảm bảo bảo mật
        new Thread(() -> {
            try {
                System.out.println("[Service] Starting SSL/TLS Chat Server (Port " + NetworkConstants.TCP_SSL_PORT + ")...");
                var sslContext = SSLUtil.createServerSSLContext("server.jks", "changeme");
                SSLServerSocket sslServerSocket = SSLUtil.createSSLServerSocket(
                    NetworkConstants.TCP_SSL_PORT,
                    sslContext
                );
                System.out.println("[Service] SSL/TLS Chat Server is running on port " + NetworkConstants.TCP_SSL_PORT);

                while (true) {
                    try {
                        SSLSocket sslClientSocket = (SSLSocket) sslServerSocket.accept();
                        String cipherSuite = sslClientSocket.getSession().getCipherSuite();
                        System.out.println("New SSL client connected from " + sslClientSocket.getInetAddress() +
                                                 " using cipher: " + cipherSuite);

                        ServerHandler handler = new ServerHandler(sslClientSocket);
                        pool.execute(handler);

                    } catch (Exception e) {
                        System.err.println("Error accepting SSL client: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to create SSL socket: " + e.getMessage());
                System.err.println("Make sure server.jks exists. Run 'create-keystore.bat' first.");
                System.err.println("SSL/TLS server will not be available.");
            }
        }).start();

        // Main thread chờ để giữ server chạy
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            System.out.println("Server shutting down...");
            pool.shutdown();
        }
    }
}