package com.chat.client.controller;

import com.chat.client.network.TcpClient;
import com.chat.client.network.HttpClientUtil; // [QUAN TRỌNG] Import cái này
import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser; // Import hộp thoại chọn file

import java.io.File;

public class ChatController {
    @FXML private ListView<String> userList;
    @FXML private TextArea txtMessageArea;
    @FXML private TextArea txtNotification;
    @FXML private TextField txtInput;

    private TcpClient client;
    private String myName;
    private java.util.Map<String, String> userIPMap = new java.util.HashMap<>(); // Map username -> IP

    public void setClient(TcpClient client, String name) {
        this.client = client;
        this.myName = name;
        // Multicast listener sẽ được khởi động từ ConnectController
        client.setOnMessageReceived(this::processMessage);
    }

    public TextArea getNotificationArea() {
        return txtNotification;
    }

    private void processMessage(ChatMessage msg) {
        switch (msg.getOpCode()) {
            case CHAT_MSG:
            case CHAT_GROUP:
                txtMessageArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
                break;
            case USER_LIST:
                // Parse users với IP: "user1:IP1,user2:IP2"
                String[] usersWithIP = msg.getContent().split(",");
                java.util.List<String> usernames = new java.util.ArrayList<>();
                userIPMap.clear();
                
                for (String userInfo : usersWithIP) {
                    String[] parts = userInfo.split(":");
                    if (parts.length >= 2) {
                        String username = parts[0];
                        String ip = parts[1];
                        usernames.add(username);
                        userIPMap.put(username, ip);
                    } else if (parts.length == 1) {
                        // Fallback: chỉ có username, không có IP
                        usernames.add(parts[0]);
                    }
                }
                
                Platform.runLater(() -> {
                    userList.getItems().clear();
                    userList.getItems().addAll(usernames);
                });
                break;
        }
    }

    @FXML
    public void handleSend(ActionEvent event) {
        String content = txtInput.getText();
        if (content.isEmpty()) return;
        ChatMessage msg = new ChatMessage(OpCode.CHAT_GROUP, myName, content);
        client.sendMessage(msg);
        txtInput.clear();
    }

    @FXML
    public void handleBuzz(ActionEvent event) {
        String selectedUser = userList.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            txtMessageArea.appendText(">>> Chọn một người để BUZZ!\n");
            return;
        }

        // Lấy IP của user được chọn
        String targetIP = getUserIP(selectedUser);
        
        if (targetIP == null) {
            // Nếu không có IP, thử dùng localhost (cho test trên cùng máy)
            targetIP = "127.0.0.1";
            txtMessageArea.appendText(">>> Gửi BUZZ tới " + selectedUser + " (localhost)...\n");
        } else {
            txtMessageArea.appendText(">>> Gửi BUZZ tới " + selectedUser + " (" + targetIP + ")\n");
        }

        // Gửi BUZZ
        if (client != null) {
            client.sendBuzz(targetIP);
        } else {
            txtMessageArea.appendText(">>> Lỗi: Client chưa được khởi tạo!\n");
        }
    }

    private String getUserIP(String username) {
        // Trả về IP từ map, hoặc null nếu không có
        return userIPMap.get(username);
    }

    // --- [MỚI THÊM] Hàm xử lý nút Gửi File ---
    @FXML
    public void handleSendFile(ActionEvent event) {
        // 1. Mở cửa sổ chọn file của Windows
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để gửi");
        File file = fileChooser.showOpenDialog(txtInput.getScene().getWindow());

        if (file != null) {
            txtMessageArea.appendText(">>> Đang tải file lên: " + file.getName() + "...\n");

            // 2. Chạy luồng riêng để upload file (tránh đơ giao diện)
            new Thread(() -> {
                try {
                    // Gọi hàm upload trong HttpClientUtil (Giả sử hàm đó tên là uploadFile)
                    // Nếu tên hàm bên file kia khác, bạn sửa lại tên hàm ở đây nhé
                    String fileUrl = HttpClientUtil.uploadFile(file, "http://localhost:8080/api/upload");

                    if (fileUrl != null) {
                        // 3. Upload xong thì gửi link cho mọi người
                        String content = "Đã gửi file: " + fileUrl;
                        ChatMessage msg = new ChatMessage(OpCode.CHAT_GROUP, myName, content);
                        client.sendMessage(msg);
                    } else {
                        Platform.runLater(() -> txtMessageArea.appendText(">>> Lỗi: Không nhận được link file.\n"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> txtMessageArea.appendText(">>> Lỗi gửi file: " + e.getMessage() + "\n"));
                    e.printStackTrace();
                }
            }).start();
        }
    }
}