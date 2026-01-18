package com.chat.client.controller;

import com.chat.client.network.TcpClient;
import com.chat.client.network.UdpDiscovery;
import com.chat.common.protocol.NetworkConstants;
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
                    // Tự động dùng port SSL (8889) nếu discovery trả về port TCP (8888)
                    if (parts.length > 1 && parts[1].equals(String.valueOf(NetworkConstants.TCP_PORT))) {
                        txtPort.setText(String.valueOf(NetworkConstants.TCP_SSL_PORT));
                        lblStatus.setText("Đã tìm thấy Server! (SSL/TLS)");
                    } else {
                        txtPort.setText(parts.length > 1 ? parts[1] : String.valueOf(NetworkConstants.TCP_SSL_PORT));
                        lblStatus.setText("Đã tìm thấy Server!");
                    }
                } else {
                    // Mặc định dùng SSL port
                    txtPort.setText(String.valueOf(NetworkConstants.TCP_SSL_PORT));
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
        
        // Xác định có dùng SSL không (port 8889 = SSL, port 8888 = TCP thường)
        boolean useSSL = (port == NetworkConstants.TCP_SSL_PORT);
        
        boolean connected = client.connect(ip, port, name, useSSL);

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

                // Kích hoạt các tính năng: UDP Buzz, Multicast
                client.initBuzzListener(stage);
                
                // Lấy TextArea từ ChatController để hiển thị multicast notifications
                javafx.scene.control.TextArea notificationArea = chatController.getNotificationArea();
                if (notificationArea != null) {
                    client.startMulticastListener(notificationArea);
                } else {
                    client.startMulticastListener(null); // Console mode
                }

            } catch (IOException e) {
                e.printStackTrace();
                lblStatus.setText("Lỗi tải giao diện Chat!");
            }
        } else {
            lblStatus.setText("Không thể kết nối tới Server!");
        }
    }
}