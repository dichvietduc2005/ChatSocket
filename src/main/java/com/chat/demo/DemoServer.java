package com.chat.demo;

import com.chat.server.network.UdpDiscoveryServer;
import com.chat.server.network.MulticastAdminServer;
import com.chat.common.crypto.SSLUtil;
import com.chat.common.protocol.NetworkConstants;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demo Server - Chạy tất cả services: Discovery + Multicast + SSL/TLS
 */
public class DemoServer {
    public static void main(String[] args) {
        System.out.println("=== ChatSocket Server Demo ===\n");

        try {
            // 1. Khởi động UDP Discovery Server
            System.out.println("[1] Starting UDP Discovery Server...");
            UdpDiscoveryServer discoveryServer = new UdpDiscoveryServer();
            discoveryServer.start();
            System.out.println("✓ UDP Discovery started on port " + NetworkConstants.UDP_DISCOVERY_PORT + "\n");

            // 2. Khởi động Multicast Admin Server
            System.out.println("[2] Starting Multicast Admin Server...");
            MulticastAdminServer multicastServer = new MulticastAdminServer();
            multicastServer.start();
            System.out.println("✓ Multicast Admin started on " + NetworkConstants.MULTICAST_ADDRESS +
                             ":" + NetworkConstants.MULTICAST_PORT + "\n");

            // 3. Khởi động SSL/TLS Server
            System.out.println("[3] Starting SSL/TLS Server...");
            try {
                var sslContext = SSLUtil.createServerSSLContext("server.jks", "changeme");
                SSLServerSocket serverSocket = SSLUtil.createSSLServerSocket(
                    NetworkConstants.TCP_SSL_PORT,
                    sslContext
                );
                System.out.println("✓ SSL/TLS Server started on port " + NetworkConstants.TCP_SSL_PORT + "\n");

                ExecutorService executor = Executors.newFixedThreadPool(10);

                // Thread để accept SSL connections
                new Thread(() -> {
                    while (true) {
                        try {
                            SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                            String cipherSuite = clientSocket.getSession().getCipherSuite();
                            System.out.println("✓ New client connected from " + clientSocket.getInetAddress() +
                                             " using cipher: " + cipherSuite);

                            executor.execute(new DemoClientHandler(clientSocket));
                        } catch (Exception e) {
                            System.err.println("Error accepting client: " + e.getMessage());
                        }
                    }
                }).start();

            } catch (Exception e) {
                System.err.println("Failed to create SSL socket: " + e.getMessage());
                System.err.println("Make sure server.jks exists. Run 'create-keystore.bat' first.");
                return;
            }

            // 4. Interactive console untuk gửi admin notifications
            System.out.println("\n=== Server Ready ===");
            System.out.println("You can type messages to broadcast via Multicast:");
            System.out.println("Type 'exit' to shutdown\n");

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    String input = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(input)) {
                        System.out.println("\nShutting down...");
                        discoveryServer.stop();
                        multicastServer.stop();
                        System.out.println("Server stopped.");
                        break;
                    }
                    if (!input.trim().isEmpty()) {
                        multicastServer.sendAdminNotification(input);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
