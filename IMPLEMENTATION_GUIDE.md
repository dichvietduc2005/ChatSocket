# ChatSocket Implementation Guide

Hướng dẫn sử dụng các tính năng đã triển khai: UDP Buzz, Broadcast Discovery, Multicast Admin, SSL/TLS

## 1. UDP Buzz - Rung cửa sổ & Âm thanh

### Tính năng
- Gửi gói tin UDP Unicast để rung cửa sổ
- Phát âm thanh (beep 800Hz) khi nhận BUZZ
- Cửa sổ rung lên 10 lần với amplitude 5px

### Cách sử dụng

#### Khởi động Buzz Listener (Client startup)
```java
TcpClient tcpClient = new TcpClient();
Stage primaryStage = new Stage();
tcpClient.initBuzzListener(primaryStage);  // Lắng nghe BUZZ từ port 9998
```

#### Gửi BUZZ đến client khác
```java
String targetIP = "192.168.1.100";  // IP của client đích
tcpClient.sendBuzz(targetIP);
```

#### Dừng listener (Application shutdown)
```java
tcpClient.stopBuzzListener();
```

### Port
- **UDP Port 9998** (NetworkConstants.UDP_BUZZ_PORT)

---

## 2. Broadcast Discovery - Tự động tìm Server

### Tính năng
- Client gửi UDP broadcast "WHERE_IS_SERVER?" đến 255.255.255.255
- Server lắng nghe và trả lời IP của nó
- Client tự động điền server address

### Cách sử dụng

#### Server - Khởi động Discovery Server
```java
UdpDiscoveryServer discoveryServer = new UdpDiscoveryServer();
discoveryServer.start();  // Lắng nghe port 9999

// Dừng khi shutdown
discoveryServer.stop();
```

#### Client - Tìm Server
```java
// Cách 1: Timeout mặc định 3000ms
UdpDiscovery.discoverServer()
    .thenAccept(serverAddress -> {
        if (serverAddress != null) {
            System.out.println("Found server at: " + serverAddress);
            // Kết nối đến server
        } else {
            System.out.println("Server not found");
        }
    });

// Cách 2: Custom timeout
UdpDiscovery.discoverServer(5000)  // 5 seconds
    .thenAccept(serverAddress -> {
        // ...
    });
```

### Port
- **UDP Port 9999** (NetworkConstants.UDP_DISCOVERY_PORT)

### Discovery Protocol
- Request: `WHERE_IS_SERVER?`
- Response: `I_AM_SERVER` (từ server IP)

---

## 3. Multicast Admin - Thông báo Admin cho tất cả Client

### Tính năng
- Admin gửi thông báo qua Multicast (địa chỉ 230.0.0.1)
- Tất cả client lắng nghe địa chỉ này
- Thông báo hiển thị dòng chạy ngang trên UI

### Cách sử dụng

#### Server - Khởi động Multicast Server
```java
MulticastAdminServer multicastServer = new MulticastAdminServer();
multicastServer.start();  // Bắt đầu lắng nghe input từ console

// Gửi thông báo lập trình
multicastServer.sendAdminNotification("Server sẽ bảo trì từ 10:00 AM");

// Dừng khi shutdown
multicastServer.stop();
```

#### Client - Lắng nghe Admin Notifications
```java
TcpClient tcpClient = new TcpClient();
TextArea notificationArea = new TextArea();  // JavaFX TextArea

tcpClient.startMulticastListener(notificationArea);

// Thông báo sẽ tự động hiển thị trong TextArea với format: 🔔 [message]

// Dừng khi shutdown
tcpClient.stopMulticastListener();
```

### Port
- **Multicast Address**: 230.0.0.1
- **Multicast Port**: 9997 (NetworkConstants.MULTICAST_PORT)

### Format Tin nhắn
- Format gửi: `ADMIN:message content`
- Format nhận: `ADMIN:` prefix được strip, chỉ hiển thị content

---

## 4. SSL/TLS - Mã hóa truyền thông

### Tính năng
- Tất cả TCP traffic được mã hóa TLS
- Tạo keystore tự ký (self-signed)
- Hỗ trợ cả server (keystore) và client (truststore)

### Setup - Tạo Keystore

#### Windows
```bash
create-keystore.bat
```

#### Linux/macOS
```bash
chmod +x create-keystore.sh
./create-keystore.sh
```

**Kết quả:**
- `server.jks` - Server keystore (chứa private key)
- `client-truststore.jks` - Client truststore (chứa public cert)
- `server.cer` - Certificate (có thể xóa)
- Password: `changeme`

### Cách sử dụng

#### Server - Khởi động SSL ServerSocket
```java
import com.chat.common.crypto.SSLUtil;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLContext;

SSLContext sslContext = SSLUtil.createServerSSLContext("server.jks", "changeme");
SSLServerSocket sslServerSocket = SSLUtil.createSSLServerSocket(
    NetworkConstants.TCP_SSL_PORT,  // Port 8889
    sslContext
);

while (true) {
    SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
    System.out.println("Client connected via: " + sslSocket.getSession().getCipherSuite());
    
    // Handle như socket thường
    new Thread(new ServerHandler(sslSocket)).start();
}
```

#### Client - Kết nối SSL
```java
import com.chat.common.crypto.SSLUtil;
import javax.net.ssl.SSLSocket;

// Option 1: Trust all (demo only)
SSLContext sslContext = SSLUtil.createClientSSLContext();

// Option 2: Use truststore (recommended)
// SSLContext sslContext = SSLUtil.createClientSSLContext("client-truststore.jks", "changeme");

SSLSocket sslSocket = SSLUtil.createSSLSocket(
    "192.168.1.100",  // Server IP
    NetworkConstants.TCP_SSL_PORT,  // Port 8889
    sslContext
);

System.out.println("Connected securely with: " + sslSocket.getSession().getCipherSuite());

// Sử dụng SSL socket
ObjectOutputStream oos = new ObjectOutputStream(sslSocket.getOutputStream());
ObjectInputStream ois = new ObjectInputStream(sslSocket.getInputStream());
```

### Port
- **SSL/TLS Port**: 8889 (NetworkConstants.TCP_SSL_PORT)
- **Normal TCP Port**: 8888

### Xác minh Encryption

Dùng Wireshark để kiểm tra:
1. Port 8888 → plaintext messages (readable)
2. Port 8889 → encrypted TLS handshake + ciphertext (not readable)

```bash
# Filter trong Wireshark
tcp.port == 8889
```

---

## Integration Checklist

### Client Startup
```java
public class ClientMain extends Application {
    private TcpClient tcpClient;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        tcpClient = new TcpClient();
        
        // 1. Khởi động UDP Buzz listener
        tcpClient.initBuzzListener(primaryStage);
        
        // 2. Khởi động Multicast listener (nếu có TextArea)
        TextArea notificationArea = new TextArea();
        tcpClient.startMulticastListener(notificationArea);
        
        // 3. Tự động tìm server
        UdpDiscovery.discoverServer()
            .thenAccept(serverAddress -> {
                if (serverAddress != null) {
                    System.out.println("Connecting to: " + serverAddress);
                    connectToServer(serverAddress);  // Kết nối TCP/SSL
                }
            });
        
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();
    }
    
    private void shutdown() {
        tcpClient.stopBuzzListener();
        tcpClient.stopMulticastListener();
    }
}
```

### Server Startup
```java
public class ServerMain {
    public static void main(String[] args) {
        // 1. Khởi động UDP Discovery Server
        UdpDiscoveryServer discoveryServer = new UdpDiscoveryServer();
        discoveryServer.start();
        
        // 2. Khởi động Multicast Admin Server
        MulticastAdminServer multicastServer = new MulticastAdminServer();
        multicastServer.start();
        
        // 3. Khởi động TCP Server (SSL hoặc normal)
        try {
            SSLContext sslContext = SSLUtil.createServerSSLContext("server.jks", "changeme");
            SSLServerSocket serverSocket = SSLUtil.createSSLServerSocket(
                NetworkConstants.TCP_SSL_PORT,
                sslContext
            );
            
            ExecutorService executor = Executors.newFixedThreadPool(10);
            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                executor.execute(new ServerHandler(clientSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## Network Configuration Summary

| Feature | Protocol | Port | Address |
|---------|----------|------|---------|
| TCP Chat (Normal) | TCP | 8888 | Server IP |
| TCP Chat (Secured) | SSL/TLS | 8889 | Server IP |
| UDP Discovery | UDP Broadcast | 9999 | 255.255.255.255 |
| UDP Buzz | UDP Unicast | 9998 | Target Client IP |
| Multicast Admin | UDP Multicast | 9997 | 230.0.0.1 |

---

## Troubleshooting

### 1. Discovery không tìm thấy server
- Kiểm tra firewall cho UDP port 9999
- Kiểm tra server đang chạy `UdpDiscoveryServer`
- Chắc chắn broadcast address là 255.255.255.255

### 2. Buzz không được nghe
- Kiểm tra firewall cho UDP port 9998
- Kiểm tra `initBuzzListener()` được gọi
- Sử dụng IP address chính xác của target client

### 3. Multicast không nhận thông báo
- Kiểm tra multicast address 230.0.0.1 được hỗ trợ
- Kiểm tra firewall cho UDP port 9997
- Chắc chắn `startMulticastListener()` được gọi trước khi server gửi

### 4. SSL Connection Refused
- Kiểm trap server.jks tồn tại và có đúng password
- Port 8889 không bị firewall chặn
- Chắc chắn server đang lắng nghe port 8889

---

## Files Reference

- `TcpClient.java` - UDP Buzz + Multicast listener
- `UdpDiscovery.java` - Client discovery logic
- `UdpDiscoveryServer.java` - Server discovery listener
- `MulticastAdminServer.java` - Admin notification server
- `SSLUtil.java` - SSL/TLS utilities
- `create-keystore.bat/sh` - Keystore generation scripts
- `SSL_SETUP.md` - Detailed SSL setup guide
