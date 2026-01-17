package com.chat.client.controller;

import com.chat.client.network.TcpClient;
import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.OpCode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

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

        // Kích hoạt lắng nghe Multicast (Admin thông báo)
        client.startMulticastListener(txtNotification);

        // Đăng ký nhận tin nhắn TCP
        client.setOnMessageReceived(this::processMessage);
    }

    private void processMessage(ChatMessage msg) {
        // Xử lý các loại tin nhắn khác nhau
        switch (msg.getOpCode()) {
            case CHAT_MSG:
            case CHAT_GROUP:
                txtMessageArea.appendText(msg.getSender() + ": " + msg.getContent() + "\n");
                break;
            case USER_LIST:
                // Cập nhật list user online (Giả sử nội dung là các tên cách nhau bởi dấu phẩy)
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

        // Chọn gửi Group hoặc 1-1 (Ở đây làm mặc định là Group cho đơn giản)
        // Nếu muốn 1-1: Lấy item đang chọn từ userList làm receiver
        ChatMessage msg = new ChatMessage(OpCode.CHAT_GROUP, myName, content);

        client.sendMessage(msg);
        txtInput.clear();
    }

    @FXML
    public void handleBuzz(ActionEvent event) {
        // Gửi Buzz tới người đang được chọn trong List
        String selectedUser = userList.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            // Lưu ý: Để Buzz hoạt động, UserList cần chứa IP, hoặc Server phải hỗ trợ chuyển tiếp Buzz
            // Ở đây code mẫu gửi tạm tin nhắn Text thông báo
            txtMessageArea.appendText(">>> Bạn vừa gửi BUZZ tới " + selectedUser + "\n");
            // client.sendBuzz("IP_CUA_NGUOI_DO"); // Cần logic lấy IP
        } else {
            txtMessageArea.appendText(">>> Chọn một người để BUZZ!\n");
        }
    }
}