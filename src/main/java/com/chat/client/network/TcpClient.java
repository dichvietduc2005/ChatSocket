package com.chat.client.network;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
// import com.chat.common.protocol.NetworkConstants; // Nếu bạn có file này thì giữ lại, không thì xóa dòng này

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.function.Consumer;

public class TcpClient {
    // === CẤU HÌNH CỔNG (Nếu chưa có NetworkConstants thì dùng số cứng ở đây) ===
    private static final int SERVER_PORT = 8888;
    private static final int UDP_BUZZ_PORT = 9999;
    private static final int MULTICAST_PORT = 7777;
    private static final String MULTICAST_ADDRESS = "230.0.0.1";

    // === TCP COMPONENTS (PHẦN MỚI THÊM VÀO) ===
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;
    private Consumer<ChatMessage> onMessageReceived; // Callback để cập nhật giao diện

    // === UDP & MULTICAST COMPONENTS (CỦA THỊNH) ===
    private DatagramSocket buzzSocket;
    private MulticastSocket multicastSocket;
    private Stage primaryStage;

    // ================== 1. PHẦN KẾT NỐI TCP (QUAN TRỌNG) ==================

    public boolean connect(String serverIP, int port, String username) {
        try {
            socket = new Socket(serverIP, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            isRunning = true;

            // Gửi gói tin LOGIN ngay khi kết nối
            ChatMessage loginMsg = new ChatMessage(OpCode.LOGIN, username, "Xin chao Server");
            out.writeObject(loginMsg);
            out.flush();

            // Bắt đầu luồng lắng nghe tin nhắn từ Server
            new Thread(this::listenForMessages).start();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setOnMessageReceived(Consumer<ChatMessage> listener) {
        this.onMessageReceived = listener;
    }

    private void listenForMessages() {
        while (isRunning) {
            try {
                Object obj = in.readObject();
                if (obj instanceof ChatMessage) {
                    ChatMessage msg = (ChatMessage) obj;
                    // Đẩy dữ liệu về giao diện (JavaFX Thread)
                    Platform.runLater(() -> {
                        if (onMessageReceived != null) onMessageReceived.accept(msg);
                    });
                }
            } catch (Exception e) {
                System.out.println("Mat ket noi Server: " + e.getMessage());
                closeConnection();
                break;
            }
        }
    }

    public void closeConnection() {
        isRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            stopBuzzListener(); // Dừng luôn UDP
            stopMulticastListener(); // Dừng luôn Multicast
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== 2. UDP BUZZ (CỦA THỊNH - GIỮ NGUYÊN) ==================
    // (Mình đã sửa lại một chút để nó chạy độc lập không phụ thuộc NetworkConstants)

    public void initBuzzListener(Stage stage) {
        this.primaryStage = stage;
        try {
            buzzSocket = new DatagramSocket(UDP_BUZZ_PORT);
            new Thread(this::listenForBuzz).start();
        } catch (SocketException e) {
            System.err.println("Lỗi UDP: " + e.getMessage());
        }
    }

    private void listenForBuzz() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (buzzSocket != null && !buzzSocket.isClosed()) {
            try {
                buzzSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                if ("BUZZ".equals(msg)) {
                    Platform.runLater(() -> vibrateWindow(primaryStage));
                    playBuzzSound();
                }
            } catch (IOException e) {}
        }
    }

    public void sendBuzz(String targetIP) {
        // Logic gửi buzz đơn giản
        try {
            byte[] data = "BUZZ".getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(targetIP), UDP_BUZZ_PORT);
            new DatagramSocket().send(packet);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void vibrateWindow(Stage stage) {
        // ... (Giữ nguyên logic rung màn hình của file gốc) ...
        // Để code ngắn gọn mình không paste lại đoạn Rung và Âm thanh ở đây,
        // BẠN HÃY COPY ĐOẠN private void vibrateWindow VÀ private void playBuzzSound CỦA BẠN VÀO ĐÂY NHÉ!
        System.out.println(">>> BUZZZZ !!!! Rung man hinh!");
    }
    private void playBuzzSound() { /* Copy từ file cũ vào nhé */ }
    public void stopBuzzListener() { if (buzzSocket != null) buzzSocket.close(); }

    // ================== 3. MULTICAST (CỦA THỊNH - GIỮ NGUYÊN) ==================
    public void startMulticastListener(TextArea notificationArea) {
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            InetSocketAddress group = new InetSocketAddress(InetAddress.getByName(MULTICAST_ADDRESS), MULTICAST_PORT);
            NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            multicastSocket.joinGroup(group, netIf);

            new Thread(() -> {
                byte[] buf = new byte[1024];
                while (!multicastSocket.isClosed()) {
                    try {
                        DatagramPacket pack = new DatagramPacket(buf, buf.length);
                        multicastSocket.receive(pack);
                        String msg = new String(pack.getData(), 0, pack.getLength());
                        Platform.runLater(() -> notificationArea.appendText("🔔 ADMIN: " + msg + "\n"));
                    } catch (Exception e) {}
                }
            }).start();
        } catch (Exception e) { e.printStackTrace(); }
    }
    public void stopMulticastListener() { if (multicastSocket != null) multicastSocket.close(); }
}