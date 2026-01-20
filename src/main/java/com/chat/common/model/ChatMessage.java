package com.chat.common.model;

import com.chat.common.protocol.OpCode;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một gói tin chat được truyền giữa Client và Server.
 * Implements Serializable để có thể gửi qua
 * ObjectOutputStream/ObjectInputStream.
 */
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private OpCode opCode; // Loại lệnh
    private String sender; // Người gửi (Tên hoặc Email)
    private String receiver; // Người nhận (Username/Email hoặc GroupId)
    private String content; // Nội dung tin nhắn
    private LocalDateTime timestamp; // Thời gian gửi
    private String senderEmail; // [MỚI] Email của người gửi (để gửi tin nhắn offline qua mail)

    // Constructor đầy đủ
    public ChatMessage(OpCode opCode, String sender, String receiver, String content) {
        this.opCode = opCode;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Constructor đơn giản (không cần receiver)
    public ChatMessage(OpCode opCode, String sender, String content) {
        this(opCode, sender, null, content);
    }

    // Getters and Setters
    public OpCode getOpCode() {
        return opCode;
    }

    public void setOpCode(OpCode opCode) {
        this.opCode = opCode;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: %s",
                opCode, sender, receiver != null ? receiver : "ALL", content);
    }
}
