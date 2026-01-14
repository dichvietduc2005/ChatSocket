package com.chat.server.memory;

import com.chat.server.core.ServerHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RAMStorage {
    // Key: Username, Value: Handler (để gửi tin nhắn lại cho user đó)
    public static final Map<String, ServerHandler> onlineUsers = new ConcurrentHashMap<>();

    /**
     * Đăng ký người dùng mới online
     */
    public static void registerUser(String username, ServerHandler handler) {
        onlineUsers.put(username, handler);
        System.out.println("User registered: " + username);
    }

    /**
     * Xóa người dùng khi offline
     */
    public static void removeUser(String username) {
        if (username != null) {
            onlineUsers.remove(username);
            System.out.println("User removed: " + username);
        }
    }

    /**
     * Lấy danh sách tên người dùng đang online
     */
    public static String getOnlineUsersString() {
        return String.join(",", onlineUsers.keySet());
    }
}
