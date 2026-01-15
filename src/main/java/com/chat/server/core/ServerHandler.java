package com.chat.server.core;

import com.chat.common.model.ChatMessage;
import com.chat.server.memory.RAMStorage;
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
import java.util.List;
import java.util.regex.Pattern;

public class ServerHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    // --- CẤU HÌNH GRPC CENSOR BOT ---
    // Sử dụng static để chia sẻ kênh kết nối (tránh tạo quá nhiều kết nối)
    private static ManagedChannel censorChannel;
    private static CensorServiceGrpc.CensorServiceBlockingStub censorStub;

    static {
        // Kết nối tới Bot Server chạy tại localhost:50051
        censorChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext() // Tắt SSL
                .build();
        censorStub = CensorServiceGrpc.newBlockingStub(censorChannel);
        System.out.println("[ServerHandler] Initialized connection to Censor Bot.");
    }
    // --------------------------------

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

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
        switch (msg.getOpCode()) {
            case LOGIN:
                this.username = msg.getSender();
                RAMStorage.registerUser(this.username, this);
                break;

            case LOGOUT:
                closeConnection();
                break;

            case CHAT_MSG: // Chat 1-1
                // ---> LỌC TỪ BẬY (Sử dụng getContent/setContent) <---
                String cleanMsgPrivate = filterProfanity(msg.getContent());
                msg.setContent(cleanMsgPrivate);
                // ----------------------------------------------------

                String receiver = msg.getReceiver();
                ServerHandler receiverHandler = RAMStorage.onlineUsers.get(receiver);
                if (receiverHandler != null) {
                    receiverHandler.send(msg);
                } else {
                    System.out.println("User not found: " + receiver);
                }
                break;

            case CHAT_GROUP: // Broadcast
                // ---> LỌC TỪ BẬY (Sử dụng getContent/setContent) <---
                String cleanMsgGroup = filterProfanity(msg.getContent());
                msg.setContent(cleanMsgGroup);
                // ----------------------------------------------------

                for (ServerHandler handler : RAMStorage.onlineUsers.values()) {
                    if (!handler.username.equals(this.username)) {
                        handler.send(msg);
                    }
                }
                break;

            default:
                System.out.println("Unknown OpCode: " + msg.getOpCode());
        }
    }

    /**
     * Gọi gRPC Bot để kiểm tra và thay thế từ bậy
     */
    private String filterProfanity(String originalText) {
        if (originalText == null || originalText.isEmpty()) return originalText;

        try {
            // Tạo request gRPC
            CensorProto.TextRequest request = CensorProto.TextRequest.newBuilder()
                    .setText(originalText)
                    .build();

            // Gửi sang Bot
            CensorProto.ProfanityResponse response = censorStub.checkProfanity(request);

            // Xử lý kết quả trả về
            if (response.getHasProfanity()) {
                String filteredText = originalText;
                List<String> badWords = response.getDetectedWordsList();

                // Thay thế các từ bậy bằng "***"
                for (String word : badWords) {
                    filteredText = filteredText.replaceAll("(?i)" + Pattern.quote(word), "***");
                }
                return filteredText;
            }
        } catch (Exception e) {
            // Log lỗi nhưng không làm crash server, trả về tin nhắn gốc
            System.err.println("⚠ Lỗi gọi Censor Bot: " + e.getMessage());
        }
        return originalText;
    }

    public synchronized void send(ChatMessage msg) throws IOException {
        out.writeObject(msg);
        out.flush();
    }

    private void closeConnection() {
        if (username != null) {
            RAMStorage.removeUser(username);
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}