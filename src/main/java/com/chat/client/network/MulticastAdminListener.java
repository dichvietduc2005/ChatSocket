package com.chat.client.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.function.Consumer;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.NetworkConstants;
import com.chat.common.protocol.OpCode;

import javafx.application.Platform;

/**
 * Lắng nghe thông báo Admin từ Multicast (230.0.0.1:9997)
 * Khi nhận được, gọi callback để hiển thị trên GUI
 */
public class MulticastAdminListener {
    private MulticastSocket multicastSocket;
    private InetAddress multicastGroup;
    private boolean isRunning = false;
    private Thread listenerThread;
    private Consumer<ChatMessage> onAdminMessageReceived;

    public void start(Consumer<ChatMessage> callback) throws IOException {
        this.onAdminMessageReceived = callback;
        this.multicastSocket = new MulticastSocket(NetworkConstants.MULTICAST_PORT);
        this.multicastGroup = InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS);
        
        multicastSocket.joinGroup(multicastGroup);
        isRunning = true;

        listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.setName("MulticastAdminListener");
        listenerThread.start();

        System.out.println("[Admin Listener] Joined multicast group: " + NetworkConstants.MULTICAST_ADDRESS + 
                         ":" + NetworkConstants.MULTICAST_PORT);
    }

    private void listen() {
        byte[] buffer = new byte[4096];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String data = new String(packet.getData(), 0, packet.getLength());
                System.out.println("[Admin] Raw data: " + data);

                // Format: "ADMIN:message content"
                if (data.startsWith("ADMIN:")) {
                    String content = data.substring(6); // Strip "ADMIN:" prefix
                    
                    // Tạo ChatMessage từ Admin
                    ChatMessage adminMsg = new ChatMessage(
                        OpCode.ADMIN_NOTIFICATION,
                        "ADMIN",
                        null,
                        content
                    );

                    // Gọi callback trên JavaFX thread
                    if (onAdminMessageReceived != null) {
                        Platform.runLater(() -> onAdminMessageReceived.accept(adminMsg));
                    }

                    System.out.println("[Admin] Received admin notification: " + content);
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("[Admin Listener] Error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                multicastSocket.leaveGroup(multicastGroup);
                multicastSocket.close();
            } catch (IOException e) {
                System.err.println("[Admin Listener] Error closing socket: " + e.getMessage());
            }
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        System.out.println("[Admin Listener] Stopped");
    }
}
