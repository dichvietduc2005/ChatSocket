package com.chat.server.core;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerManager {
    // Danh sách tất cả các Client đang kết nối (TCP + SSL)
    private static final Set<ServerHandler> clients = ConcurrentHashMap.newKeySet();

    // [QUAN TRỌNG] Lưu lịch sử 50 tin nhắn
    private static final List<ChatMessage> chatHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;

    // Đăng ký Client mới
    public static void addClient(ServerHandler client) {
        clients.add(client);
    }

    // Xóa Client
    public static void removeClient(ServerHandler client) {
        clients.remove(client);
        broadcastUserList();
    }

    // Gửi tin nhắn cho tất cả (Có lưu lịch sử)
    public static void broadcast(ChatMessage msg, ServerHandler sender) {
        // 1. Lưu vào lịch sử nếu là chat nhóm
        if (msg.getOpCode() == OpCode.CHAT_GROUP) {
            saveToHistory(msg);
        }

        // 2. Gửi cho tất cả
        for (ServerHandler client : clients) {
            client.send(msg);
        }
    }

    // Gửi tin nhắn riêng
    public static void sendPrivate(ChatMessage msg, String receiverName, ServerHandler sender) {
        boolean found = false;
        for (ServerHandler client : clients) {
            if (client.getUsername() != null && client.getUsername().equals(receiverName)) {
                client.send(msg);
                sender.send(msg); // Gửi lại cho người gửi để họ thấy
                found = true;
                break;
            }
        }
    }

    // Gửi danh sách user online
    public static void broadcastUserList() {
        StringBuilder sb = new StringBuilder();
        // Luôn thêm Chat Tổng vào đầu
        for (ServerHandler client : clients) {
            if (client.getUsername() != null) {
                sb.append(client.getUsername()).append(":OK,");
            }
        }
        String content = sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        ChatMessage msg = new ChatMessage(OpCode.USER_LIST, "Server", null, content);
        
        for (ServerHandler client : clients) {
            client.send(msg);
        }
    }

    // [LOGIC LƯU TRỮ]
    private static synchronized void saveToHistory(ChatMessage msg) {
        chatHistory.add(msg);
        if (chatHistory.size() > MAX_HISTORY) {
            chatHistory.remove(0);
        }
    }

    // [LOGIC TẢI LẠI] Gửi lịch sử cho 1 người cụ thể (dùng khi Login)
    public static void sendHistoryTo(ServerHandler client) {
        for (ChatMessage msg : chatHistory) {
            client.send(msg);
        }
    }
}