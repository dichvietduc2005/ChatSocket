package com.chat.server.network;

import com.chat.common.protocol.NetworkConstants;
import java.net.*;
import java.io.IOException;
import java.util.Scanner;

public class MulticastAdminServer {
    private MulticastSocket socket;
    private InetAddress multicastGroup;
    private boolean isRunning = false;
    private Thread senderThread;

    public void start() {
        try {
            socket = new MulticastSocket();
            multicastGroup = InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS);

            isRunning = true;
            System.out.println("Multicast Admin Server started");
            System.out.println("Multicast address: " + NetworkConstants.MULTICAST_ADDRESS +
                             ":" + NetworkConstants.MULTICAST_PORT);
            System.out.println("Type messages to broadcast to all clients (type 'exit' to stop):");

            senderThread = new Thread(this::sendAdminMessages);
            senderThread.setDaemon(true);
            senderThread.start();

        } catch (IOException e) {
            System.err.println("Failed to start Multicast Admin Server: " + e.getMessage());
        }
    }

    public void sendAdminNotification(String message) {
        if (!isRunning || socket == null || socket.isClosed()) {
            System.err.println("Multicast server is not running");
            return;
        }

        try {
            String notification = "ADMIN:" + message;
            byte[] data = notification.getBytes();

            DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                multicastGroup,
                NetworkConstants.MULTICAST_PORT
            );

            socket.send(packet);
            System.out.println("Sent admin notification: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send admin notification: " + e.getMessage());
        }
    }

    private void sendAdminMessages() {
        Scanner scanner = new Scanner(System.in);
        while (isRunning) {
            try {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    stop();
                    break;
                }
                if (!input.trim().isEmpty()) {
                    sendAdminNotification(input);
                }
            } catch (Exception e) {
                if (isRunning) {
                    System.err.println("Error reading input: " + e.getMessage());
                }
            }
        }
        scanner.close();
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (senderThread != null) {
            senderThread.interrupt();
        }
        System.out.println("Multicast Admin Server stopped");
    }
}
