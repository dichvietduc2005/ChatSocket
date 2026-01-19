package com.chat.server.core;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
// import com.chat.server.service.EmailService; // Tạm tắt nếu chưa có class này
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
        try {
            // Kết nối tới Bot Server (Port 50051)
            censorChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
                    .usePlaintext()
                    .build();
            censorStub = CensorServiceGrpc.newBlockingStub(censorChannel);
            System.out.println("[ServerHandler] Connected to Censor Bot.");
        } catch (Exception e) {
            System.err.println("[ServerHandler] Warning: Censor Bot not available.");
        }
    }
    // --------------------------------

    public ServerHandler(Socket socket) {
        this.socket = socket;
        this.sslSocket = null;
    }

    public ServerHandler(SSLSocket sslSocket) {
        this.sslSocket = sslSocket;
        this.socket = sslSocket;
    }

    public String getClientIP() {
        if (socket != null && !socket.isClosed()) {
            return socket.getInetAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            if (sslSocket != null) {
                out = new ObjectOutputStream(sslSocket.getOutputStream());
                in = new ObjectInputStream(sslSocket.getInputStream());
            } else {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            }

            // Đăng ký kết nối (nhưng chưa có tên)
            ServerManager.addClient(this);

            while (true) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof ChatMessage) {
                        ChatMessage msg = (ChatMessage) obj;
                        if (msg.getOpCode() == OpCode.LOGOUT) {
                            System.out.println("[System] Người dùng chủ động đăng xuất: " + username);
                            break;
                        }
                        handleMessage(msg);
                    }
                } catch (SocketException e) {
                    System.err.println("[!!!] Mất kết nối đột ngột (SocketException) với: "
                            + (username != null ? username : "Unknown"));
                    com.chat.server.network.WebSocketServer.broadcastLog("System: Connection reset for " + username);
                    break;
                } catch (EOFException e) {
                    System.err.println("[!!!] Luồng dữ liệu bị ngắt quãng (EOF) với: "
                            + (username != null ? username : "Unknown"));
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Network] Error in ServerHandler for " + username + ": " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(ChatMessage msg) throws IOException {
        // [1. LỌC TỪ BẬY]
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            String cleanMsg = filterProfanity(msg.getContent());
            msg.setContent(cleanMsg);
        }

        // [2. XỬ LÝ THEO OPCODE]
        switch (msg.getOpCode()) {
            case LOGIN:
                this.username = msg.getSender();
                System.out.println("User logged in: " + username);

                // Cập nhật danh sách Online cho mọi người
                ServerManager.broadcastUserList();

                // [QUAN TRỌNG] Gửi lại 50 tin nhắn lịch sử cho người mới vào
                ServerManager.sendHistoryTo(this);
                break;

            case LOGOUT:
                // Sẽ được xử lý ở vòng lặp run()
                break;

            case CHAT_MSG: // Chat Riêng 1-1
                if (msg.getReceiver() != null) {
                    ServerManager.sendPrivate(msg, msg.getReceiver(), this);
                } else {
                    // Nếu lỗi client gửi private mà không có người nhận -> chuyển thành Broadcast
                    msg.setOpCode(OpCode.CHAT_GROUP);
                    handleMessage(msg);
                }
                break;

            case CHAT_GROUP: // Chat Tổng (Broadcast)
                ServerManager.broadcast(msg, this);
                break;

            default:
                break;
        }
    }

    // Gửi tin nhắn xuống Client
    public synchronized void send(ChatMessage msg) {
        try {
            if (!socket.isClosed()) {
                out.writeObject(msg);
                out.flush();
            }
        } catch (IOException e) {
            // Lỗi mạng -> Client coi như đã mất kết nối
        }
    }

    // Logic gọi sang gRPC Bot để lọc từ bậy
    private String filterProfanity(String originalText) {
        if (originalText == null || censorStub == null)
            return originalText;
        try {
            // Chỉ lọc Text, không lọc URL ảnh/file (tránh làm hỏng link)
            if (originalText.startsWith("http"))
                return originalText;

            CensorProto.TextRequest request = CensorProto.TextRequest.newBuilder()
                    .setText(originalText)
                    .build();
            CensorProto.ProfanityResponse response = censorStub.checkProfanity(request);

            if (response.getHasProfanity()) {
                return "*** [CENSORED] ***";
            }
        } catch (Exception e) {
            // Nếu Bot chết, cho qua tin nhắn (Fail-open)
        }
        return originalText;
    }

    private void closeConnection() {
        System.out.println("[Cleanup] Đang giải phóng tài nguyên cho: " + (username != null ? username : "Unknown"));
        ServerManager.removeClient(this); // Xóa khỏi danh sách quản lý
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