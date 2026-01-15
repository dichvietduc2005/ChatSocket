package com.chat.server;

import com.chat.bot.CensorBotServer;
import com.chat.server.core.ServerHandler;
import com.chat.server.network.HttpFileServer;
import com.chat.server.network.MulticastAdminServer;
import com.chat.server.network.UdpDiscoveryServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int CHAT_PORT = 8888;
    private static final int MAX_THREADS = 100;

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        System.out.println(">>> ĐANG KHỞI ĐỘNG HỆ THỐNG CHAT SERVER ĐA DỊCH VỤ <<<");

        // --- 1. KHỞI CHẠY CENSOR BOT (gRPC) ---
        // Bot chạy ở Port 50051 để lọc từ bậy
        new Thread(() -> {
            try {
                System.out.println("[Service] Starting Censor Bot (Port 50051)...");
                // Gọi hàm main của Bot để chạy gRPC Server
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

        // --- 3. KHỞI CHẠY UDP DISCOVERY ---
        // Giúp Client quét mạng LAN để tìm thấy Server
        new Thread(() -> {
            System.out.println("[Service] Starting UDP Discovery Server (Port 8889)...");
            new UdpDiscoveryServer().start();
        }).start();

        // --- 4. KHỞI CHẠY MULTICAST ADMIN (Tùy chọn) ---
        new Thread(() -> {
            System.out.println("[Service] Starting Multicast Admin Server (Port 8890)...");
            new MulticastAdminServer().start();
        }).start();

        // --- 5. KHỞI CHẠY CHAT SERVER (Main Thread) ---
        // Server chạy ở Port 8888 để xử lý tin nhắn
        try (ServerSocket serverSocket = new ServerSocket(CHAT_PORT)) {
            System.out.println("[Service] Chat Server is running on port " + CHAT_PORT);
            System.out.println("[System] Waiting for clients...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Tạo handler cho client mới
                    // Handler này đã tích hợp:
                    // + Gọi Censor Bot để lọc chat
                    // + Gọi EmailService nếu user offline
                    ServerHandler handler = new ServerHandler(clientSocket);
                    pool.execute(handler);

                } catch (IOException e) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + CHAT_PORT);
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}