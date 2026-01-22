package com.chat.client.controller;

import com.chat.client.network.TcpClient;
import com.chat.client.network.HttpUploadClient;
import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatController {
    @FXML private ListView<String> userList;
    @FXML private TextArea txtMessageArea;
    @FXML private TextArea txtNotification;
    @FXML private TextField txtInput;

    private TcpClient client;
    private String myName;
    private Map<String, String> userIPMap = new HashMap<>();

    public void setClient(TcpClient client, String name) {
        this.client = client;
        this.myName = name;
        client.setOnMessageReceived(this::processMessage);

        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                if (newVal == null || newVal.equals("Mọi người")) {
                    txtInput.setPromptText("Nhập tin nhắn gửi tới Mọi người...");
                } else {
                    txtInput.setPromptText("Đang nhắn tin riêng cho [" + newVal + "]...");
                }
            });
        });
    }

    // [QUAN TRỌNG] Hàm này nãy bị thiếu, giờ đã thêm lại để ConnectController không báo lỗi
    public TextArea getNotificationArea() {
        return txtNotification;
    }

    private void processMessage(ChatMessage msg) {
        // [MỚI] Lấy giờ hiện tại
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        switch (msg.getOpCode()) {
            case CHAT_MSG:
                if (msg.getSender().equals(myName)) {
                    txtMessageArea.appendText("[" + time + "] [Riêng tới " + msg.getReceiver() + "]: " + msg.getContent() + "\n");
                } else {
                    txtMessageArea.appendText("[" + time + "] [Riêng từ " + msg.getSender() + "]: " + msg.getContent() + "\n");
                }
                break;
            case CHAT_GROUP:
                txtMessageArea.appendText("[" + time + "] " + msg.getSender() + ": " + msg.getContent() + "\n");
                break;
            case USER_LIST:
                String[] usersWithIP = msg.getContent().split(",");
                List<String> usernames = new ArrayList<>();
                userIPMap.clear();

                for (String userInfo : usersWithIP) {
                    String[] parts = userInfo.split(":");
                    if (parts.length >= 2) {
                        usernames.add(parts[0]);
                        userIPMap.put(parts[0], parts[1]);
                    } else if (parts.length == 1) {
                        usernames.add(parts[0]);
                    }
                }

                Platform.runLater(() -> {
                    String selected = userList.getSelectionModel().getSelectedItem();
                    userList.getItems().clear();
                    userList.getItems().add("Mọi người");
                    for (String u : usernames) {
                        if (!u.trim().isEmpty()) userList.getItems().add(u.trim());
                    }
                    if (selected != null && userList.getItems().contains(selected)) {
                        userList.getSelectionModel().select(selected);
                    } else {
                        userList.getSelectionModel().select("Mọi người");
                    }
                });
                break;
        }
    }

    @FXML
    public void handleSend(ActionEvent event) {
        String content = txtInput.getText();
        if (content.isEmpty()) return;

        String selectedUser = userList.getSelectionModel().getSelectedItem();
        if (selectedUser != null && !selectedUser.equals("Mọi người") && !selectedUser.equals(myName)) {
            ChatMessage msg = new ChatMessage(OpCode.CHAT_MSG, myName, content);
            msg.setReceiver(selectedUser);
            client.sendMessage(msg);
        } else {
            ChatMessage msg = new ChatMessage(OpCode.CHAT_GROUP, myName, content);
            client.sendMessage(msg);
        }
        txtInput.clear();
    }

    @FXML
    public void handleBuzz(ActionEvent event) {
        String selectedUser = userList.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            txtMessageArea.appendText(">>> Chọn một người để BUZZ!\n");
            return;
        }
        String targetIP = userIPMap.get(selectedUser);
        if (targetIP == null) targetIP = "127.0.0.1";

        txtMessageArea.appendText(">>> Gửi BUZZ tới " + selectedUser + "...\n");
        if (client != null) client.sendBuzz(targetIP);
    }

    @FXML
    public void handleSendFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để gửi");
        File file = fileChooser.showOpenDialog(txtInput.getScene().getWindow());

        if (file != null) {
            txtMessageArea.appendText(">>> Đang tải file lên: " + file.getName() + "...\n");
            new Thread(() -> {
                try {
                    String fileUrl = HttpUploadClient.uploadFile(file, "http://localhost:8080/api/upload");
                    if (fileUrl != null) {
                        String content = "Đã gửi file: " + fileUrl;
                        ChatMessage msg = new ChatMessage(OpCode.CHAT_GROUP, myName, content);
                        client.sendMessage(msg);
                    } else {
                        Platform.runLater(() -> txtMessageArea.appendText(">>> Lỗi: Không nhận được link file.\n"));
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> txtMessageArea.appendText(">>> Lỗi gửi file: " + e.getMessage() + "\n"));
                }
            }).start();
        }
    }
}