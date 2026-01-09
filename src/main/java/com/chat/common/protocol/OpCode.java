package com.chat.common.protocol;

/**
 * Định nghĩa các mã lệnh (Operation Codes) cho giao thức Chat.
 * Dùng chung cho cả Client và Server.
 */
public enum OpCode {
    // === TCP Opcodes ===
    LOGIN, // Định danh người dùng (gửi Tên|Email)
    LOGOUT, // Đăng xuất
    CHAT_MSG, // Tin nhắn 1-1 (private)
    CHAT_GROUP, // Tin nhắn nhóm (broadcast)
    SECURE_CHAT, // Tin nhắn qua SSL/TLS
    FILE_TRANSFER, // Thông báo gửi file (chứa link HTTP)

    // === UDP Opcodes ===
    DISCOVERY_REQ, // Client hỏi "Server đâu?" (Broadcast)
    DISCOVERY_RES, // Server trả lời IP/Port
    BUZZ, // Tính năng rung cửa sổ (UDP Unicast)

    // === Multicast Opcodes ===
    ADMIN_NOTIFICATION, // Tin nhắn từ Admin (Multicast 230.0.0.1)

    // === Status Opcodes ===
    SUCCESS, // Thành công
    ERROR, // Lỗi
    USER_LIST // Danh sách user online
}
