package com.chat.server.core;

import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
import com.chat.server.memory.RAMStorage;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ServerHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Setup streams (out before in to prevent deadlock)
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
                // Gửi lại thông báo thành công hoặc danh sách user (tùy chọn)
                break;

            case LOGOUT:
                closeConnection();
                break;

            case CHAT_MSG: // Chat 1-1
                String receiver = msg.getReceiver();
                ServerHandler receiverHandler = RAMStorage.onlineUsers.get(receiver);
                if (receiverHandler != null) {
                    receiverHandler.send(msg);
                } else {
                    // Có thể gửi lại tin nhắn báo lỗi User offline nếu cần
                    System.out.println("User not found: " + receiver);
                }
                break;

            case CHAT_GROUP: // Broadcast
                for (ServerHandler handler : RAMStorage.onlineUsers.values()) {
                    // Không gửi lại cho chính mình (tùy logic client, ở đây cứ gửi hết hoặc trừ
                    // mình)
                    if (!handler.username.equals(this.username)) {
                        handler.send(msg);
                    }
                }
                break;

            default:
                System.out.println("Unknown OpCode: " + msg.getOpCode());
        }
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
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
