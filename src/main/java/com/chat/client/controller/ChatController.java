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

    public void setClient(TcpClient client, String name) {
        this.client = client;
        this.myName = name;
        client.startMulticastListener(txtNotification);
        client.setOnMessageReceived(this::processMessage);
    }

    private void processMessage(ChatMessage msg) {
        switch (msg.getOpCode()) {
            case CHAT_MSG:
            case CHAT_GROUP:
                txtMessageArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
                break;
            case USER_LIST:
                String[] users = msg.getContent().split(",");
                Platform.runLater(() -> {
                    userList.getItems().clear();
                    userList.getItems().addAll(users);
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
        if (selectedUser != null) {
            txtMessageArea.appendText(">>> Bạn vừa gửi BUZZ tới " + selectedUser + "\n");
        } else {
            txtMessageArea.appendText(">>> Chọn một người để BUZZ!\n");
        }
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