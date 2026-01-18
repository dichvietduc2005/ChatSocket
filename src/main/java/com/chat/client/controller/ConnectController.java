package com.chat.client.controller;

import com.chat.client.network.TcpClient;
import com.chat.client.network.UdpDiscovery;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ConnectController {
    @FXML
    private TextField txtIp;
    @FXML
    private TextField txtPort;
    @FXML
    private TextField txtName;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnConnect;

    @FXML
    public void initialize() {
        lblStatus.setText("Đang tìm Server trong mạng nội bộ...");
        UdpDiscovery.discoverServer().thenAccept(serverAddr -> {
            javafx.application.Platform.runLater(() -> {
                if (serverAddr != null) {
                    String[] parts = serverAddr.split(":");
                    txtIp.setText(parts[0]);
                    txtPort.setText(parts[1]);
                    lblStatus.setText("Đã tìm thấy Server!");
                } else {
                    lblStatus.setText("Không tìm thấy Server tự động. Hãy nhập tay.");
                }
            });
        });
    }

    @FXML
    public void handleConnect(ActionEvent event) {
        String ip = txtIp.getText();
        String name = txtName.getText();
        int port;

        try {
            port = Integer.parseInt(txtPort.getText());
        } catch (NumberFormatException e) {
            lblStatus.setText("Port phải là số!");
            return;
        }

        if (name.isEmpty()) {
            lblStatus.setText("Vui lòng nhập tên!");
            return;
        }

        // Khởi tạo Client và kết nối
        TcpClient client = new TcpClient();
        boolean connected = client.connect(ip, port, name);

        if (connected) {
            try {
                // Chuyển sang màn hình Chat
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/chat/client/view/chat_view.fxml"));
                Parent root = loader.load();

                // Lấy ChatController để truyền đối tượng client sang
                ChatController chatController = loader.getController();
                chatController.setClient(client, name);

                Stage stage = (Stage) btnConnect.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Chat Room - " + name);

                // Kích hoạt tính năng UDP Rung cửa sổ
                client.initBuzzListener(stage);

            } catch (IOException e) {
                e.printStackTrace();
                lblStatus.setText("Lỗi tải giao diện Chat!");
            }
        } else {
            lblStatus.setText("Không thể kết nối tới Server!");
        }
    }
}