package com.chat.server.core;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
import com.chat.server.memory.RAMStorage;
import com.chat.server.network.WebSocketServer;
import com.chat.server.service.EmailService;
import com.chat.grpc.CensorProto;
import com.chat.grpc.CensorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import javax.net.ssl.SSLSocket;
import java.util.List;
import java.util.regex.Pattern;

public class ServerHandler implements Runnable {
    private Socket socket;
    private SSLSocket sslSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    // --- CẤU HÌNH GRPC CENSOR BOT ---
    private static ManagedChannel censorChannel;
    private static CensorServiceGrpc.CensorServiceBlockingStub censorStub;

    static {
        // Kết nối tới Bot Server
        censorChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        censorStub = CensorServiceGrpc.newBlockingStub(censorChannel);
        System.out.println("[ServerHandler] Đã kết nối tới Bot Kiểm Duyệt.");
    }
    // --------------------------------

    public ServerHandler(Socket socket) {
        this.socket = socket;
        this.sslSocket = null;
    }

    public ServerHandler(SSLSocket sslSocket) {
        this.sslSocket = sslSocket;
        this.socket = sslSocket; // SSLSocket extends Socket
    }

    @Override
    public void run() {
        try {
            // Hỗ trợ cả Socket thường và SSLSocket
            if (sslSocket != null) {
                // SSL connection - server tạo ObjectOutputStream trước
                out = new ObjectOutputStream(sslSocket.getOutputStream());
                in = new ObjectInputStream(sslSocket.getInputStream());
            } else {
                // TCP thường
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            }

            while (true) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof ChatMessage) {
                        ChatMessage msg = (ChatMessage) obj;
                        handleMessage(msg);
                    }
                } catch (EOFException | SocketException e) {
                    System.out.println("Client disconnected: " + username);
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(ChatMessage msg) throws IOException {
        // [LỌC TỪ BẬY]
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            String cleanMsg = filterProfanity(msg.getContent());
            msg.setContent(cleanMsg);
        }

        // Gửi log ra Web (Bỏ qua nếu lỗi)
        try {
            WebSocketServer.broadcastLog("[" + msg.getOpCode() + "] " + msg.getSender() + ": " + msg.getContent());
        } catch (Exception e) {
        }

        switch (msg.getOpCode()) {
            case LOGIN:
                this.username = msg.getSender();
                RAMStorage.registerUser(this.username, this);
                broadcastUserList();
                break;
            case LOGOUT:
                closeConnection();
                break;
            case CHAT_MSG: // Chat 1-1
                ServerHandler receiver = RAMStorage.onlineUsers.get(msg.getReceiver());
                if (receiver != null) {
                    receiver.send(msg);
                    // Gửi ngược lại cho chính người gửi để họ thấy tin nhắn mình vừa nhắn
                    this.send(msg);
                }
                break;

            // === [QUAN TRỌNG] ĐÃ SỬA PHẦN NÀY ĐỂ BẠN TỰ THẤY TIN NHẮN CỦA MÌNH ===
            case CHAT_GROUP:
                for (ServerHandler handler : RAMStorage.onlineUsers.values()) {
                    // Đã xóa dòng if chặn người gửi. Giờ gửi cho TẤT CẢ mọi người.
                    handler.send(msg);
                }
                break;
        }
    }

    /**
     * Hàm lọc từ bậy (Logic giữ nguyên)
     */
    private String filterProfanity(String originalText) {
        try {
            CensorProto.TextRequest request = CensorProto.TextRequest.newBuilder()
                    .setText(originalText)
                    .build();

            CensorProto.ProfanityResponse response = censorStub.checkProfanity(request);

            if (response.getHasProfanity()) {
                return "*** Tin nhắn vi phạm ***";
            }
        } catch (Exception e) {
            System.err.println("⚠ Lỗi gọi Bot: " + e.getMessage());
        }
        return originalText;
    }

    public synchronized void send(ChatMessage msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    private void broadcastUserList() {
        // Tạo danh sách users kèm IP: "user1:IP1,user2:IP2"
        String usersWithIP = RAMStorage.onlineUsers.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    ServerHandler handler = entry.getValue();
                    String ip = handler.getClientIP();
                    return username + ":" + ip;
                })
                .collect(java.util.stream.Collectors.joining(","));

        ChatMessage listMsg = new ChatMessage(OpCode.USER_LIST, "SERVER", usersWithIP);
        for (ServerHandler handler : RAMStorage.onlineUsers.values()) {
            try {
                handler.send(listMsg);
            } catch (IOException e) {
                // handler closed
            }
        }
    }

    public String getClientIP() {
        if (socket != null && !socket.isClosed()) {
            return socket.getInetAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    private void closeConnection() {
        if (username != null) {
            RAMStorage.removeUser(username);
            broadcastUserList();
        }
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
        }
    }
}