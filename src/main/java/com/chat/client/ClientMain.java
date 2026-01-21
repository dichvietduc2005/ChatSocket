package com.chat.client;

import java.awt.Toolkit;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLSocket;

import com.chat.client.network.HttpUploadClient;
import com.chat.client.network.MulticastAdminListener;
import com.chat.client.network.UdpDiscovery;
import com.chat.common.crypto.SSLUtil;
import com.chat.common.model.ChatMessage;
import com.chat.common.protocol.NetworkConstants;
import com.chat.common.protocol.OpCode;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ClientMain extends Application {

    // --- CẤU HÌNH ---
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8889; // SSL/TLS Port
    private static final String UPLOAD_API_URL = "http://localhost:8080/upload";
    private static final String CHAT_GENERAL_KEY = "📢 CHAT TỔNG";

    // --- UI COMPONENTS ---
    private Stage mainStage; // [THAY ĐỔI 1] Biến toàn cục để dùng khi mở popup ảnh
    private ListView<ChatMessage> chatWindow;
    private ListView<String> userListWindow;
    private TextField inputField;
    private Label statusLabel;
    private Label currentTargetLabel;
    private Button buzzBtn;
    
    // --- DATA ---
    private Map<String, ObservableList<ChatMessage>> conversations = new HashMap<>();
    private ObservableList<String> onlineUsers = FXCollections.observableArrayList();
    private Set<String> currentOnlineSet = new HashSet<>();
    private String currentReceiver = null; 
    private Map<String, String> userIpMap = new HashMap<>();
    private String serverHost = SERVER_HOST;
    private int serverPort = SERVER_PORT;
    
    // --- NETWORK ---
    private SSLSocket sslSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private String userEmail; // [MỚI] Email người dùng
    
    // --- BUZZ ---
    private DatagramSocket buzzSocket;
    
    // --- MULTICAST ADMIN LISTENER ---
    private MulticastAdminListener adminListener;

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;

        // 1. Dialog Login với hiển thị IP/Port
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Đăng nhập");
        dialog.setHeaderText("Hệ thống Chat Pro (SSL/TLS)");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField usernameField = new TextField("User" + (int)(Math.random() * 999));
        usernameField.setPromptText("Nhập tên hiển thị");
        
        TextField emailField = new TextField(); // [MỚI] Email field
        emailField.setPromptText("Nhập email (để gửi tin nhắn offline)");
        
        Label ipLabel = new Label("Server IP:");
        TextField ipField = new TextField(serverHost);
        ipField.setEditable(true);
        
        Label portLabel = new Label("Server Port:");
        TextField portField = new TextField(String.valueOf(serverPort));
        portField.setEditable(true);
        
        Label infoLabel = new Label("Kết nối SSL/TLS mã hóa \n đang tìm server trong mạng nội bộ...");
        infoLabel.setStyle("-fx-text-fill: #0084ff; -fx-font-weight: bold;");
        
        grid.add(new Label("Tên hiển thị:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1); // [MỚI]
        grid.add(emailField, 1, 1); // [MỚI]
        grid.add(ipLabel, 0, 2);
        grid.add(ipField, 1, 2);
        grid.add(portLabel, 0, 3);
        grid.add(portField, 1, 3);
        grid.add(infoLabel, 0, 4, 2, 1);
        
        ButtonType loginButtonType = new ButtonType("Kết nối", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(grid);
        
        Platform.runLater(() -> usernameField.requestFocus());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return usernameField.getText();
            }
            return null;
        });
        
        // Tự động tìm Server qua UDP Discovery (prefill IP/Port nếu tìm thấy)
        UdpDiscovery.discoverServer().thenAccept(addr -> {
            Platform.runLater(() -> {
                if (addr != null && addr.contains(":")) {
                    String[] parts = addr.split(":");
                    ipField.setText(parts[0]);
                    // Nếu server trả TCP (8888), tự động chuyển sang SSL (8889)
                    if (parts.length > 1 && parts[1].equals(String.valueOf(NetworkConstants.TCP_PORT))) {
                        portField.setText(String.valueOf(NetworkConstants.TCP_SSL_PORT));
                    } else if (parts.length > 1) {
                        portField.setText(parts[1]);
                    }
                    infoLabel.setText("Đã tìm thấy server: " + ipField.getText() + ":" + portField.getText());
                } else {
                    infoLabel.setText("Không tìm thấy tự động. Bạn có thể nhập IP/Port.");
                }
            });
        });

        var result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            username = result.get().trim();
            userEmail = emailField.getText().trim(); // [MỚI] Lưu email
            serverHost = ipField.getText().trim().isEmpty() ? SERVER_HOST : ipField.getText().trim();
            try {
                serverPort = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                serverPort = SERVER_PORT;
            }
        } else {
            return;
        }

        // --- KHỞI TẠO DỮ LIỆU & UI COMPONENTS TRƯỚC (FIX LỖI NULL POINTER) ---
        conversations.put(CHAT_GENERAL_KEY, FXCollections.observableArrayList());
        conversations.put("🔔 ADMIN CHAT", FXCollections.observableArrayList()); // [MỚI] Khởi tạo Admin Chat conversation
        
        // [QUAN TRỌNG] Khởi tạo chatWindow ngay tại đây để tránh lỗi khi Sidebar gọi tới nó
        chatWindow = new ListView<>();
        chatWindow.setStyle("-fx-background-color: #f0f2f5; -fx-control-inner-background: #f0f2f5;");
        chatWindow.setCellFactory(param -> new ChatMessageCell());
        chatWindow.setItems(conversations.get(CHAT_GENERAL_KEY));

        // 2. Giao diện chính
        BorderPane root = new BorderPane();
        
        // --- HEADER ---
        HBox header = new HBox(10);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #fff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 0, 2, 5, 0);");
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label logoLabel = new Label("💬 JChat");
        logoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        logoLabel.setTextFill(Color.web("#0084ff"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        currentTargetLabel = new Label("Đang chat: " + CHAT_GENERAL_KEY);
        currentTargetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        header.getChildren().addAll(logoLabel, spacer, currentTargetLabel);
        root.setTop(header);

        // --- LEFT SIDEBAR ---
        VBox leftSide = new VBox();
        leftSide.setPrefWidth(160);
        leftSide.setStyle("-fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");
        
        Label userListTitle = new Label("Trực tuyến");
        userListTitle.setPadding(new Insets(10));
        userListTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #888;");
        
        userListWindow = new ListView<>(onlineUsers);
        userListWindow.setCellFactory(param -> new UserListCell());
        userListWindow.setStyle("-fx-background-color: transparent; -fx-control-inner-background: #fff;");
        VBox.setVgrow(userListWindow, Priority.ALWAYS);

        // Sự kiện chọn User
        userListWindow.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.equals(CHAT_GENERAL_KEY)) {
                    currentReceiver = null;
                    currentTargetLabel.setText("Đang chat: " + CHAT_GENERAL_KEY);
                    currentTargetLabel.setTextFill(Color.BLACK);
                } else {
                    if (newVal.equals(username)) return;
                    currentReceiver = newVal;
                    currentTargetLabel.setText("Đang chat riêng: " + newVal);
                    currentTargetLabel.setTextFill(Color.web("#6200ea"));
                }
                switchChatData(newVal); // Giờ gọi hàm này an toàn rồi vì chatWindow đã có
                
                // Enable/Disable nút Buzz
                if (buzzBtn != null) {
                    buzzBtn.setDisable(currentReceiver == null || newVal.equals(CHAT_GENERAL_KEY) || newVal.equals(username));
                }
            }
        });

        onlineUsers.add(CHAT_GENERAL_KEY);
        onlineUsers.add("🔔 ADMIN CHAT"); // [MỚI] Thêm Admin Chat channel
        userListWindow.getSelectionModel().select(0); // Kích hoạt sự kiện chọn

        leftSide.getChildren().addAll(userListTitle, userListWindow);
        root.setLeft(leftSide);

        // --- CENTER ---
        // Không cần new ListView ở đây nữa vì đã new ở trên đầu rồi
        root.setCenter(chatWindow);

        // --- FOOTER ---
        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setStyle("-fx-background-color: #fff;");
        bottomBox.setAlignment(Pos.CENTER);
        
        Button fileBtn = new Button("📎");
        fileBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: #0084ff;");
        fileBtn.setTooltip(new Tooltip("Gửi file hoặc ảnh"));
        fileBtn.setOnAction(e -> selectAndUploadFile(primaryStage));
        
        buzzBtn = new Button("🔔 BUZZ");
        buzzBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #ff4444; -fx-font-weight: bold;");
        buzzBtn.setTooltip(new Tooltip("Rung cửa sổ người nhận"));
        buzzBtn.setOnAction(e -> sendBuzz());
        buzzBtn.setDisable(true); // Chỉ enable khi chọn user riêng
        
        inputField = new TextField();
        inputField.setPromptText("Nhập tin nhắn...");
        inputField.setStyle("-fx-background-radius: 20px; -fx-padding: 10px; -fx-background-color: #f0f2f5;");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage());

        Button sendBtn = new Button("Gửi");
        sendBtn.setStyle("-fx-background-color: #0084ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px;");
        sendBtn.setOnAction(e -> sendMessage());

        bottomBox.getChildren().addAll(fileBtn, buzzBtn, inputField, sendBtn);
        root.setBottom(bottomBox);

        // 3. Kết nối
        new Thread(this::connectToServer).start();

        // 4. Show Window
        Scene scene = new Scene(root, 750, 550);
        primaryStage.setTitle("Chat Client: " + username);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> closeConnection());
        primaryStage.show();
    }
    
    private void switchChatData(String key) {
        // [FIX LỖI] Kiểm tra null để an toàn tuyệt đối
        if (chatWindow == null) return; 

        conversations.putIfAbsent(key, FXCollections.observableArrayList());
        chatWindow.setItems(conversations.get(key));
        chatWindow.scrollTo(chatWindow.getItems().size() - 1);
    }

    // --- NETWORK LOGIC ---
    // [CẬP NHẬT] Xử lý đóng kết nối êm đẹp - SSL/TLS
    private void connectToServer() {
        try {
            // Kết nối SSL/TLS
            var sslContext = SSLUtil.createClientSSLContext();
            sslSocket = SSLUtil.createSSLSocket(serverHost, serverPort, sslContext);
            
            // Đợi một chút để SSL handshake hoàn tất
            Thread.sleep(100);
            
            // Tạo streams
            in = new ObjectInputStream(sslSocket.getInputStream());
            out = new ObjectOutputStream(sslSocket.getOutputStream());
            
            System.out.println("✓ Connected securely with cipher: " + sslSocket.getSession().getCipherSuite());

            ChatMessage loginMsg = new ChatMessage(OpCode.LOGIN, username, null, null);
            loginMsg.setSenderEmail(userEmail); // [MỚI] Gửi email kèm login
            out.writeObject(loginMsg);
            out.flush();

            // Khởi động Buzz listener
            initBuzzListener();
            
            // [MỚI] Khởi động Multicast Admin Listener
            try {
                adminListener = new MulticastAdminListener();
                adminListener.start(msg -> {
                    Platform.runLater(() -> addMessageToConversation("🔔 ADMIN CHAT", msg));
                });
            } catch (Exception e) {
                System.err.println("Failed to start Multicast Admin Listener: " + e.getMessage());
            }
            
            // Vòng lặp lắng nghe tin nhắn
            while (sslSocket != null && !sslSocket.isClosed()) {
                try {
                    Object obj = in.readObject();
                    if (obj instanceof ChatMessage) {
                        ChatMessage msg = (ChatMessage) obj;

                        if (msg.getOpCode() == OpCode.USER_LIST) {
                            updateUserList(msg.getContent());
                        } 
                        else if (msg.getOpCode() == OpCode.CHAT_GROUP) {
                            Platform.runLater(() -> addMessageToConversation(CHAT_GENERAL_KEY, msg));
                        }
                        else if (msg.getOpCode() == OpCode.CHAT_MSG) {
                            Platform.runLater(() -> {
                                String targetKey = msg.getSender().equals(username) ? msg.getReceiver() : msg.getSender();
                                addMessageToConversation(targetKey, msg);
                            });
                        }
                    }
                } catch (SocketException e) {
                    // [QUAN TRỌNG] Nếu socket đã đóng (do mình tắt app) thì thoát vòng lặp êm đẹp
                    if (sslSocket != null && sslSocket.isClosed()) {
                        System.out.println("🔴 Kết nối đã đóng.");
                        break; 
                    } else {
                        throw e; // Nếu lỗi thật thì ném ra ngoài
                    }
                } catch (EOFException e) {
                    // Server ngắt kết nối đột ngột
                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("🔴 Disconnected");
                            statusLabel.setStyle("-fx-text-fill: red;");
                        }
                    });
                    break;
                }
            }
        } catch (Exception e) {
            // Chỉ in lỗi nếu socket chưa đóng (Lỗi kết nối thật sự)
            if (sslSocket != null && !sslSocket.isClosed()) {
                e.printStackTrace();
            }
        } finally {
            stopBuzzListener();
        }
    }

    private void addMessageToConversation(String key, ChatMessage msg) {
        if (key == null) return;
        conversations.putIfAbsent(key, FXCollections.observableArrayList());
        conversations.get(key).add(msg);
        String currentTab = userListWindow.getSelectionModel().getSelectedItem();
        if (currentTab != null && currentTab.equals(key)) {
            chatWindow.scrollTo(chatWindow.getItems().size() - 1);
        }
    }

    private void updateUserList(String userStr) {
        if (userStr == null || userStr.isEmpty()) return;
        
        Platform.runLater(() -> {
            String selected = userListWindow.getSelectionModel().getSelectedItem();

            // 1. Xóa danh sách trạng thái online (Set) để cập nhật mới
            currentOnlineSet.clear();
            currentOnlineSet.add(CHAT_GENERAL_KEY);

            userIpMap.clear();

            // 2. Parse danh sách từ Server để biết ai ĐANG online
            String[] users = userStr.split(",");
            for (String u : users) {
                if (u.contains(":")) {
                    String[] parts = u.split(":");
                    String name = parts[0];
                    String ip = parts.length > 1 ? parts[1] : "127.0.0.1";
                    
                    userIpMap.put(name, ip);
                    currentOnlineSet.add(name); // Đánh dấu người này đang xanh (online)
                }
            }

            for (String activeUser : currentOnlineSet) {
                if (!onlineUsers.contains(activeUser)) {
                    onlineUsers.add(activeUser);
                }
            }

            // 4. Force Refresh để CellFactory vẽ lại màu (Xanh -> Đỏ hoặc Đỏ -> Xanh)
            userListWindow.refresh();

            // 5. Giữ lại lựa chọn cũ
            if (selected != null && onlineUsers.contains(selected)) {
                userListWindow.getSelectionModel().select(selected);
            } else {
                // Nếu chưa chọn gì thì chọn Chat Tổng
                if (userListWindow.getSelectionModel().getSelectedItem() == null) {
                    userListWindow.getSelectionModel().select(0);
                }
            }
        });
    }

    private void sendMessage() {
        String content = inputField.getText().trim();
        if (content.isEmpty()) return;
        
        // [MỚI] Không gửi nếu đang xem Admin Chat
        if ("🔔 ADMIN CHAT".equals(currentReceiver) || (currentReceiver == null && userListWindow.getSelectionModel().getSelectedItem() != null && userListWindow.getSelectionModel().getSelectedItem().equals("🔔 ADMIN CHAT"))) {
            showNotification("⚠ Không thể gửi tin nhắn tới Admin Chat. Admin Chat là kênh chỉ xem.");
            return;
        }
        
        try {
            OpCode code = (currentReceiver == null) ? OpCode.CHAT_GROUP : OpCode.CHAT_MSG;
            ChatMessage msg = new ChatMessage(code, username, currentReceiver, content);
            msg.setSenderEmail(userEmail); // [MỚI] Gửi email để gửi tin offline nếu cần
            out.writeObject(msg); out.flush();
            inputField.clear();
        } catch (IOException e) { e.printStackTrace(); }
    }
    
    private void selectAndUploadFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            new Thread(() -> {
                String fileUrl = HttpUploadClient.uploadFile(selectedFile, UPLOAD_API_URL);
                Platform.runLater(() -> {
                    if (fileUrl != null) {
                        try {
                            OpCode code = (currentReceiver == null) ? OpCode.CHAT_GROUP : OpCode.CHAT_MSG;
                            ChatMessage msg = new ChatMessage(code, username, currentReceiver, fileUrl);
                            out.writeObject(msg); out.flush();
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                });
            }).start();
        }
    }

    private void closeConnection() {
        try {
            if (out != null) {
                out.writeObject(new ChatMessage(OpCode.LOGOUT, username, null, null));
                out.flush();
            }
            if (sslSocket != null) sslSocket.close();
            stopBuzzListener();
            
            // [MỚI] Dừng Multicast Listener
            if (adminListener != null) {
                adminListener.stop();
            }
        } catch (Exception e) {}
        System.exit(0);
    }
    
    // --- BUZZ FUNCTIONS ---
    private void initBuzzListener() {
        try {
            buzzSocket = new DatagramSocket(NetworkConstants.UDP_BUZZ_PORT);
            new Thread(this::listenForBuzz).start();
            System.out.println("✓ Buzz listener started on port " + NetworkConstants.UDP_BUZZ_PORT);
        } catch (SocketException e) {
            System.err.println("⚠ Không thể khởi động Buzz Listener: Port " + NetworkConstants.UDP_BUZZ_PORT + " đã bị chiếm.");
        }
    }
    
    private void listenForBuzz() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (buzzSocket != null && !buzzSocket.isClosed()) {
            try {
                buzzSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                if ("BUZZ".equals(msg)) {
                    Platform.runLater(() -> vibrateWindow(mainStage));
                    playBuzzSound();
                }
            } catch (IOException e) {
                if (!buzzSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void sendBuzz() {
        if (currentReceiver == null || currentReceiver.equals(CHAT_GENERAL_KEY)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Buzz");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một người để BUZZ!");
            alert.showAndWait();
            return;
        }
        
        // Lấy IP từ user list (fallback localhost)
        String targetIP = userIpMap.getOrDefault(currentReceiver, "127.0.0.1");
        
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] data = "BUZZ".getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, 
                    InetAddress.getByName(targetIP), NetworkConstants.UDP_BUZZ_PORT);
            socket.send(packet);
            System.out.println(">>> Đã gửi BUZZ tới " + currentReceiver + " (" + targetIP + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void vibrateWindow(Stage stage) {
        if (stage == null) return;
        double originalX = stage.getX();
        double originalY = stage.getY();

        new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Platform.runLater(() -> {
                        stage.setX(originalX + (Math.random() * 10 - 5));
                        stage.setY(originalY + (Math.random() * 10 - 5));
                    });
                    Thread.sleep(50);
                }
                Platform.runLater(() -> {
                    stage.setX(originalX);
                    stage.setY(originalY);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println(">>> BUZZZZ !!!! Rung man hinh!");
    }
    
    private void playBuzzSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void stopBuzzListener() {
        if (buzzSocket != null && !buzzSocket.isClosed()) {
            buzzSocket.close();
        }
    }
    
    // [MỚI] Hàm hiển thị thông báo
    private void showNotification(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    // --- CELL FACTORIES ---
    private class UserListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); } else {
                HBox box = new HBox(10); box.setAlignment(Pos.CENTER_LEFT);
                Circle statusDot = new Circle(4, Color.LIMEGREEN);
                Label nameLabel = new Label(item);
                if (item.equals(CHAT_GENERAL_KEY)) {
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0084ff;");
                    statusDot.setFill(Color.ORANGE);
                } else if (currentOnlineSet.contains(item)) {
                    statusDot.setFill(Color.LIMEGREEN); // Online: Màu Xanh
                    nameLabel.setStyle("-fx-text-fill: #000;"); // Chữ đen
                    
                    if (item.equals(username)) {
                        nameLabel.setText(item + " (Bạn)");
                        nameLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #888;");
                    }
                }else {
                    // Offline: Màu Đỏ
                    statusDot.setFill(Color.RED); 
                    nameLabel.setStyle("-fx-text-fill: #999;"); // Chữ xám mờ
                }
                box.getChildren().addAll(statusDot, nameLabel); 
                setGraphic(box);
            }
        }
    }

    private class ChatMessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage msg, boolean empty) {
            super.updateItem(msg, empty);
            if (empty || msg == null) {
                setGraphic(null); setText(null);
                setStyle("-fx-background-color: transparent;"); return;
            }
            VBox vBox = new VBox(3);
            boolean isMe = msg.getSender().equals(username);
            
            if (!isMe && msg.getOpCode() == OpCode.CHAT_GROUP) {
                Label nameLabel = new Label(msg.getSender());
                nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-padding: 0 0 0 5;");
                vBox.getChildren().add(nameLabel);
            }

            String content = msg.getContent() != null ? msg.getContent() : "";
            if (isImageUrl(content)) renderImage(content, vBox);
            else if (isFileUrl(content)) renderFile(content, vBox, isMe);
            else renderText(content, vBox, isMe, msg.getOpCode() == OpCode.CHAT_MSG);

            HBox container = new HBox(vBox);
            if (isMe) { vBox.setAlignment(Pos.CENTER_RIGHT); container.setAlignment(Pos.CENTER_RIGHT); } 
            else { vBox.setAlignment(Pos.CENTER_LEFT); container.setAlignment(Pos.CENTER_LEFT); }
            setGraphic(container);
            setStyle("-fx-background-color: transparent; -fx-padding: 5px;");
        }

        // [THAY ĐỔI 2] Cập nhật hàm hiển thị ảnh để thêm sự kiện Click phóng to
        private void renderImage(String url, VBox vBox) {
            try {
                ImageView iv = new ImageView();
                iv.setFitWidth(200); 
                iv.setPreserveRatio(true);
                iv.setCursor(javafx.scene.Cursor.HAND);

                Image img = new Image(url, true);
                iv.setImage(img);
                
                // [FIX LỖI CLICK 2 LẦN]: Dùng setOnMousePressed thay vì setOnMouseClicked
                iv.setOnMousePressed(e -> {
                    // Kiểm tra chuột trái
                    if (e.isPrimaryButtonDown()) {
                        e.consume(); // Chặn ngay, không cho ListView xử lý sự kiện này nữa
                        showEnlargedImage(img);
                    }
                });

                vBox.getChildren().add(iv);
            } catch (Exception e) {}
        }

        private void renderFile(String url, VBox vBox, boolean isMe) {
            String name = url.substring(url.lastIndexOf("/") + 1);
            if(name.length() > 9 && name.charAt(8) == '_') name = name.substring(9);
            Hyperlink link = new Hyperlink("📄 " + name);
            link.setOnAction(e -> getHostServices().showDocument(url));
            vBox.getChildren().add(link);
        }

        private void renderText(String content, VBox vBox, boolean isMe, boolean isPrivate) {
            Text text = new Text(content);
            text.setFill(isMe ? Color.WHITE : Color.BLACK);
            text.wrappingWidthProperty().bind(getListView().widthProperty().subtract(150));
            TextFlow flow = new TextFlow(text);
            flow.setPadding(new Insets(8, 12, 8, 12));
            if (isPrivate) {
                if (isMe) flow.setStyle("-fx-background-color: #6200ea; -fx-background-radius: 18px;");
                else { flow.setStyle("-fx-background-color: #ede7f6; -fx-background-radius: 18px;"); text.setFill(Color.BLACK); }
            } else {
                if (isMe) flow.setStyle("-fx-background-color: #0084ff; -fx-background-radius: 18px;");
                else flow.setStyle("-fx-background-color: #e4e6eb; -fx-background-radius: 18px;");
            }
            vBox.getChildren().add(flow);
        }

        private boolean isImageUrl(String url) {
            if (url == null || !url.startsWith("http")) return false;
            String lower = url.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        }
        private boolean isFileUrl(String url) { return url != null && url.contains("/files/") && !isImageUrl(url); }
    }

    // [MỚI] Hàm hiển thị cửa sổ phóng to ảnh (Lightbox)
    private void showEnlargedImage(Image image) {
        if (image == null || image.isError()) return;

        // Tạo cửa sổ mới (Stage) dạng popup
        final Stage dialog = new Stage();
        dialog.initModality(Modality.NONE); // Không chặn cửa sổ chính
        dialog.initOwner(mainStage); // Gắn với cửa sổ chính
        dialog.setTitle("Xem ảnh");

        // Tạo ImageView lớn, tái sử dụng lại hình ảnh đã tải
        ImageView hugeImageView = new ImageView(image);
        hugeImageView.setPreserveRatio(true);

        // Tính toán kích thước tối đa dựa trên màn hình
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double maxWidth = screenBounds.getWidth() * 0.9;
        double maxHeight = screenBounds.getHeight() * 0.9;

        // Logic tự động điều chỉnh kích thước ảnh cho vừa màn hình
        if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
            hugeImageView.setFitWidth(maxWidth);
            hugeImageView.setFitHeight(maxHeight);
        }

        // Đặt ảnh vào giữa một StackPane có nền tối
        StackPane rootPane = new StackPane(hugeImageView);
        rootPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9);"); // Nền đen bán trong suốt
        rootPane.setAlignment(Pos.CENTER);

        // Đóng popup khi click vào nền hoặc nhấn ESC
        rootPane.setOnMouseClicked(e -> dialog.close());
        hugeImageView.setOnMouseClicked(e -> e.consume()); // Click vào ảnh không đóng
        
        Scene dialogScene = new Scene(rootPane);
        dialogScene.setFill(Color.TRANSPARENT); // Cần thiết để thấy nền bán trong suốt
        dialogScene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) dialog.close();
        });

        dialog.setScene(dialogScene);
        // Cấu hình cửa sổ trong suốt, không có thanh tiêu đề
        dialog.initStyle(StageStyle.TRANSPARENT); 
        dialog.setMaximized(true); // Mở toàn màn hình
        dialog.show();
    }
}