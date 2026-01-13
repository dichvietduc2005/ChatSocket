package com.chat.server.network;

import com.chat.common.protocol.NetworkConstants;
import java.net.*;
import java.io.IOException;
import java.util.Collections;

public class UdpDiscoveryServer {
    private DatagramSocket socket;
    private Thread listenerThread;
    private boolean isRunning = false;
    private String serverIP;

    public void start() {
        try {
            socket = new DatagramSocket(NetworkConstants.UDP_DISCOVERY_PORT);
            socket.setBroadcast(true);

            serverIP = getLocalNetworkIP();
            if (serverIP == null) {
                serverIP = "127.0.0.1";
            }

            isRunning = true;
            listenerThread = new Thread(this::listenForDiscovery);
            listenerThread.setDaemon(true);
            listenerThread.start();

            System.out.println("UDP Discovery Server started on port " +
                             NetworkConstants.UDP_DISCOVERY_PORT);
            System.out.println("Server IP: " + serverIP);
        } catch (SocketException e) {
            System.err.println("Failed to start UDP Discovery Server: " + e.getMessage());
        }
    }

    private void listenForDiscovery() {
        byte[] buffer = new byte[1024];

        while (isRunning && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String request = new String(packet.getData(), 0, packet.getLength()).trim();

                if (NetworkConstants.DISCOVERY_REQUEST.equals(request)) {
                    String response = NetworkConstants.DISCOVERY_RESPONSE;
                    byte[] responseData = response.getBytes();

                    DatagramPacket responsePacket = new DatagramPacket(
                        responseData,
                        responseData.length,
                        packet.getAddress(),
                        packet.getPort()
                    );

                    socket.send(responsePacket);
                    System.out.println("Responded to discovery from " + packet.getAddress());
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error in discovery server: " + e.getMessage());
                }
            }
        }
    }

    private String getLocalNetworkIP() {
        try {
            try (DatagramSocket tempSocket = new DatagramSocket()) {
                tempSocket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                return tempSocket.getLocalAddress().getHostAddress();
            }
        } catch (Exception e) {
            try {
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (!ni.isLoopback() && ni.isUp()) {
                        for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                            if (addr instanceof Inet4Address) {
                                return addr.getHostAddress();
                            }
                        }
                    }
                }
            } catch (SocketException ex) {
                System.err.println("Error getting network interfaces: " + ex.getMessage());
            }
        }
        return null;
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        System.out.println("UDP Discovery Server stopped");
    }
}
