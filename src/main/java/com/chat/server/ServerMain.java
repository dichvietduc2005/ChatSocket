package com.chat.server;

import com.chat.server.core.ServerHandler;
import com.chat.server.network.HttpFileServer; // Import server file
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int CHAT_PORT = 8888; // Port dành riêng cho Chat Socket
    private static final int MAX_THREADS = 100;

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        // --- 1. KHỞI CHẠY HTTP FILE SERVER (Chạy luồng riêng) ---
        // Lưu ý: Hãy chắc chắn SimpleFileServer.java đang dùng port KHÁC 8888 (ví dụ 8080)
        new Thread(() -> {
            System.out.println("[System] Starting HTTP File Server...");
            new HttpFileServer().start();
        }).start();
        // --------------------------------------------------------

        // --- 2. KHỞI CHẠY CHAT SERVER (Luồng chính) ---
        try (ServerSocket serverSocket = new ServerSocket(CHAT_PORT)) {
            System.out.println("[System] Chat Server is running on port " + CHAT_PORT);
            System.out.println("[System] Waiting for clients...");

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Tạo handler cho client mới và đưa vào ThreadPool
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