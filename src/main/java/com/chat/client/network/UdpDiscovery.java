package com.chat.client.network;

import com.chat.common.protocol.NetworkConstants;
import java.net.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UdpDiscovery {
    private static final int DISCOVERY_TIMEOUT_MS = 3000;

    public static CompletableFuture<String> discoverServer() {
        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(DISCOVERY_TIMEOUT_MS);

                String request = NetworkConstants.DISCOVERY_REQUEST;
                byte[] requestData = request.getBytes();

                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket requestPacket = new DatagramPacket(
                    requestData,
                    requestData.length,
                    broadcastAddress,
                    NetworkConstants.UDP_DISCOVERY_PORT
                );

                System.out.println("Broadcasting discovery request...");
                socket.send(requestPacket);

                byte[] buffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(responsePacket);
                String response = new String(
                    responsePacket.getData(),
                    0,
                    responsePacket.getLength()
                ).trim();

                if (NetworkConstants.DISCOVERY_RESPONSE.equals(response)) {
                    String serverAddress = responsePacket.getAddress().getHostAddress() +
                                         ":" + NetworkConstants.TCP_PORT;
                    System.out.println("Server found at: " + serverAddress);
                    future.complete(serverAddress);
                } else {
                    System.out.println("Invalid discovery response: " + response);
                    future.complete(null);
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Discovery timeout: No server found");
                future.complete(null);
            } catch (IOException e) {
                System.err.println("Discovery error: " + e.getMessage());
                future.complete(null);
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }).start();

        return future;
    }

    public static CompletableFuture<String> discoverServer(int timeoutMs) {
        CompletableFuture<String> future = discoverServer();

        CompletableFuture<String> timeoutFuture = new CompletableFuture<>();
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                timeoutFuture.complete(result);
            } else {
                timeoutFuture.completeExceptionally(throwable);
            }
        });

        CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (!future.isDone()) {
                    timeoutFuture.complete(null);
                }
            });

        return timeoutFuture;
    }
}
