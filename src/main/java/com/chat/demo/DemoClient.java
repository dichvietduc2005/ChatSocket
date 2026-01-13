package com.chat.demo;

import com.chat.client.network.TcpClient;
import com.chat.client.network.UdpDiscovery;
import com.chat.common.crypto.SSLUtil;
import com.chat.common.protocol.NetworkConstants;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.Scanner;

/**
 * Demo Client - Kết nối SSL + Discovery + Buzz + Multicast
 */
public class DemoClient {
    private SSLSocket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private TcpClient tcpClient;

    public static void main(String[] args) {
        System.out.println("=== ChatSocket Client Demo ===\n");

        DemoClient client = new DemoClient();
        client.run();
    }

    public void run() {
        try {
            tcpClient = new TcpClient();

            // 1. Auto-discover server
            System.out.println("[1] Discovering server...");
            String serverAddress = discoverServer();

            if (serverAddress == null) {
                System.out.println("✗ Could not find server. Using default: 127.0.0.1");
                serverAddress = "127.0.0.1:" + NetworkConstants.TCP_PORT;
            }

            String[] parts = serverAddress.split(":");
            String serverIP = parts[0];

            System.out.println("✓ Server found at: " + serverAddress + "\n");

            // 2. Connect SSL
            System.out.println("[2] Connecting to server via SSL...");
            connectSSL(serverIP);

            // 3. Initialize Buzz Listener
            System.out.println("[3] Starting Buzz listener on port " + NetworkConstants.UDP_BUZZ_PORT + "...");
            tcpClient.initBuzzListener(null); // No GUI in demo
            System.out.println("✓ Buzz listener started\n");

            // 4. Start Multicast listener
            System.out.println("[4] Starting Multicast listener...");
            tcpClient.startMulticastListener(null); // No TextArea in demo
            System.out.println("✓ Multicast listener started\n");

            System.out.println("=== Client Ready ===");
            System.out.println("Commands:");
            System.out.println("  msg <text>      - Send message to server");
            System.out.println("  buzz <ip>       - Send buzz to IP");
            System.out.println("  quit            - Disconnect\n");

            interactiveMode();

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private String discoverServer() {
        try {
            var future = UdpDiscovery.discoverServer(3000);
            return future.get();
        } catch (Exception e) {
            System.err.println("Discovery failed: " + e.getMessage());
            return null;
        }
    }

    private void connectSSL(String serverIP) throws Exception {
        var sslContext = SSLUtil.createClientSSLContext();
        socket = SSLUtil.createSSLSocket(serverIP, NetworkConstants.TCP_SSL_PORT, sslContext);

        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());

        System.out.println("✓ Connected securely with cipher: " + socket.getSession().getCipherSuite() + "\n");

        // Thread để membaca responses từ server
        new Thread(() -> readFromServer()).start();
    }

    private void readFromServer() {
        try {
            Object obj;
            while ((obj = ois.readObject()) != null) {
                System.out.println("Server: " + obj);
            }
        } catch (EOFException e) {
            System.out.println("Server disconnected");
        } catch (Exception e) {
            if (!socket.isClosed()) {
                System.err.println("Error reading from server: " + e.getMessage());
            }
        }
    }

    private void interactiveMode() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                try {
                    String input = scanner.nextLine();
                    if (input.isEmpty()) continue;

                    String[] cmd = input.split(" ", 2);
                    switch (cmd[0]) {
                        case "msg":
                            if (cmd.length > 1) {
                                oos.writeObject(cmd[1]);
                                oos.flush();
                            }
                            break;

                        case "buzz":
                            if (cmd.length > 1) {
                                tcpClient.sendBuzz(cmd[1]);
                            } else {
                                System.out.println("Usage: buzz <ip_address>");
                            }
                            break;

                        case "quit":
                            return;

                        default:
                            System.out.println("Unknown command: " + cmd[0]);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }

    private void cleanup() {
        System.out.println("\nCleaning up...");
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }

        tcpClient.stopBuzzListener();
        tcpClient.stopMulticastListener();

        System.out.println("Client stopped.");
    }
}
