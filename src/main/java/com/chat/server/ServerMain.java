package com.chat.server;

import com.chat.server.core.ServerHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 8888;
    private static final int MAX_THREADS = 100; // Hoặc số lượng tùy chỉnh

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            System.out.println("Waiting for clients...");

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
            System.err.println("Could not listen on port " + PORT);
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}
