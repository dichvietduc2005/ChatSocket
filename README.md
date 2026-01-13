# ChatSocket - Network Programming Project

**Dự án chat đa tính năng sử dụng Socket Programming với Java**

---

## 🎯 Tổng quan

ChatSocket là ứng dụng chat được xây dựng dựa trên **TCP/UDP Sockets** với các tính năng mạng nâng cao:

### ✨ Tính năng chính (Đã triển khai)

1. **TCP Chat** (1-1 & Nhóm) - Normal + SSL/TLS
2. **UDP Buzz** 🔔 - Rung cửa sổ + Âm thanh
3. **UDP Broadcast Discovery** 🔍 - Tìm server tự động
4. **UDP Multicast Admin** 📢 - Thông báo admin
5. **SSL/TLS Encryption** 🔐 - Mã hóa toàn bộ TCP

---

## 📁 Cấu trúc dự án

```
ChatSocket/
├── src/main/java/com/chat/
│   ├── client/
│   │   ├── ClientMain.java           (JavaFX entry point)
│   │   ├── controller/               (JavaFX controllers - TODO)
│   │   └── network/
│   │       ├── TcpClient.java        ✅ (UDP Buzz + Multicast)
│   │       └── UdpDiscovery.java     ✅ (Discovery client)
│   │
│   ├── server/
│   │   ├── ServerMain.java           (Server entry point - TODO)
│   │   ├── core/
│   │   │   └── ServerHandler.java    (Client handler - TODO)
│   │   ├── network/
│   │   │   ├── UdpDiscoveryServer.java    ✅
│   │   │   └── MulticastAdminServer.java  ✅
│   │   ├── memory/
│   │   │   └── RAMStorage.java       (User/Message storage - TODO)
│   │   └── service/
│   │       └── EmailService.java     (Email notifications - TODO)
│   │
│   ├── common/
│   │   ├── protocol/
│   │   │   ├── OpCode.java           (Message opcodes)
│   │   │   └── NetworkConstants.java (Port configs)
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   └── ChatMessage.java
│   │   └── crypto/
│   │       └── SSLUtil.java          ✅ (SSL utilities)
│   │
│   ├── bot/
│   │   └── CensorBotServer.java      (gRPC censor bot - TODO)
│   │
│   └── demo/                         ✅ (Demo applications)
│       ├── DemoServer.java
│       ├── DemoClient.java
│       └── DemoClientHandler.java
│
├── create-keystore.bat               ✅ (Windows keystore script)
├── create-keystore.sh                ✅ (Linux/macOS keystore script)
├── pom.xml                           (Maven dependencies)
│
└── Documentation/
    ├── README.md                     (This file)
    ├── IMPLEMENTATION_GUIDE.md       ✅ (Cách tích hợp/tận dụng tính năng)
    └── COMPLETION_REPORT.md          ✅ (Những gì đã triển khai)
```

---

## 🚀 Quick Start

### 1. Clone & Build
```bash
git clone <repo>
cd ChatSocket
mvn clean install
```

### 2. Setup SSL (One-time)
```bash
# Windows
create-keystore.bat

# Linux/macOS
chmod +x create-keystore.sh
./create-keystore.sh
```

### 3. Run Demo
```bash
# Terminal 1: Start Server
mvn exec:java -Dexec.mainClass="com.chat.demo.DemoServer"

# Terminal 2: Start Client
mvn exec:java -Dexec.mainClass="com.chat.demo.DemoClient"
```

### 4. Test Commands
```
msg hello             # Send message
buzz 192.168.1.100   # Send buzz to IP
quit                 # Exit
```

---

## 📚 Documentation

| Document | Purpose |
|----------|---------|
| **README.md** | Tổng quan & quick start |
| **IMPLEMENTATION_GUIDE.md** | Hướng dẫn triển khai & API sử dụng |
| **COMPLETION_REPORT.md** | Báo cáo chi tiết những hạng mục đã hoàn thành |

---

## 🏗️ Kiến trúc mạng

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Network Architecture                                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  TCP/SSL Layer (Reliable, Ordered)                   │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │ Port 8888: Normal TCP Chat Messages              │ │ │
│  │  │ Port 8889: SSL/TLS Encrypted Chat (RECOMMENDED) │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  UDP Broadcast Layer                                 │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │ Port 9999: Server Discovery                    │ │ │
│  │  │   Request: WHERE_IS_SERVER?                    │ │ │
│  │  │   Response: I_AM_SERVER                        │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  UDP Unicast Layer                                   │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │ Port 9998: Buzz (Window Vibration + Sound)     │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  UDP Multicast Layer                                 │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │ Address: 230.0.0.1:9997 - Admin Notifications │ │ │
│  │  │   Format: ADMIN:message text                   │ │ │
│  │  │   Scope: Local network (TTL=1)                 │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔌 Network Ports

```
TCP Ports:
  8888  - Normal TCP Chat (plaintext)
  8889  - SSL/TLS Chat (encrypted) ⭐ RECOMMENDED

UDP Ports:
  9999  - Discovery Broadcast
  9998  - Buzz Unicast (P2P)
  9997  - Multicast Admin

Reserved:
  8080  - WebSocket (for future)
  50051 - gRPC (for Censor Bot)
```

---

## ✅ Feature Status

### Triển khai hoàn tất (100%)

| # | Tính năng | File(s) | Status |
|---|-----------|---------|--------|
| 1 | **UDP Buzz** | TcpClient.java | ✅ DONE |
| 2 | **UDP Discovery** | UdpDiscovery.java + UdpDiscoveryServer.java | ✅ DONE |
| 3 | **Multicast Admin** | MulticastAdminServer.java + TcpClient.java | ✅ DONE |
| 4 | **SSL/TLS** | SSLUtil.java + keystore scripts | ✅ DONE |

### Chưa triển khai (TODO)

| # | Tính năng | File(s) | Ghi chú |
|---|-----------|---------|---------|
| 5 | TCP Chat Server | ServerMain.java, ServerHandler.java | Core logic |
| 6 | User Management | RAMStorage.java | In-memory storage |
| 7 | JavaFX GUI | ClientMain.java, Controllers, FXML | UI layer |
| 8 | File Transfer | (new files) | HTTP-based |
| 9 | Email Service | EmailService.java | Notifications |
| 10 | Censor Bot | CensorBotServer.java | gRPC service |

---

## 🧪 Testing

### Run Demo Applications
```bash
# Server
mvn exec:java -Dexec.mainClass="com.chat.demo.DemoServer"

# Client (multiple terminals)
mvn exec:java -Dexec.mainClass="com.chat.demo.DemoClient"
```

### Verify SSL Encryption (Wireshark)
```bash
# Filter traffic on SSL port
wireshark -f "tcp port 8889"

# Expected:
# - Port 8889: TLS Handshake + encrypted data
# - Port 8888: Plaintext messages (if tested)
```

### Check Ports
```bash
# Windows
netstat -ano | findstr :8889

# Linux/macOS
netstat -an | grep 8889
ss -tulpn | grep 8889
```

---

## 🔒 Security

### SSL/TLS Certificate
```bash
# Generate keystore (one-time)
create-keystore.bat          # Windows
./create-keystore.sh         # Linux/macOS

# Files created:
# - server.jks              (Server keystore - keep secret!)
# - client-truststore.jks   (Client truststore)
# - server.cer              (Can delete after setup)

# Default password: changeme
# Change for production!
```

### Production Checklist
- [ ] Use CA-signed certificates (not self-signed)
- [ ] Change keystore password from "changeme"
- [ ] Enable SSL on all TCP connections
- [ ] Validate client certificates
- [ ] Use strong ciphers (TLS 1.2+)
- [ ] Disable multicast if not needed

---

## 📖 API Reference

### UDP Buzz (Client)
```java
TcpClient tcpClient = new TcpClient();

// Start listening for buzz on port 9998
tcpClient.initBuzzListener(primaryStage);

// Send buzz to another client
tcpClient.sendBuzz("192.168.1.100");

// Stop listening
tcpClient.stopBuzzListener();
```

### Discovery (Client)
```java
// Auto-discover server
UdpDiscovery.discoverServer()
    .thenAccept(serverAddr -> {
        if (serverAddr != null) {
            // Connect to serverAddr
        }
    });

// With custom timeout
UdpDiscovery.discoverServer(5000);
```

### Discovery (Server)
```java
UdpDiscoveryServer server = new UdpDiscoveryServer();
server.start();    // Listen on port 9999
server.stop();     // Shutdown
```

### Multicast Admin (Server)
```java
MulticastAdminServer admin = new MulticastAdminServer();
admin.start();              // Interactive console
admin.sendAdminNotification("Server maintenance at 10:00");
admin.stop();
```

### Multicast Admin (Client)
```java
TcpClient tcpClient = new TcpClient();
tcpClient.startMulticastListener(notificationTextArea);
// Notifications appear automatically
tcpClient.stopMulticastListener();
```

### SSL/TLS
```java
import com.chat.common.crypto.SSLUtil;
import javax.net.ssl.SSLSocket;

// Server
SSLContext sslCtx = SSLUtil.createServerSSLContext("server.jks", "changeme");
SSLServerSocket sslServer = SSLUtil.createSSLServerSocket(8889, sslCtx);

// Client
SSLContext sslCtx = SSLUtil.createClientSSLContext();
SSLSocket sslSocket = SSLUtil.createSSLSocket("127.0.0.1", 8889, sslCtx);
```

---

## 🐛 Troubleshooting

### Discovery Timeout
```
"Discovery timeout: No server found"

Solution:
- Verify server running: UdpDiscoveryServer started
- Check firewall on port 9999
- Ensure same network (or use localhost)
```

### SSL Certificate Error
```
"SSLHandshakeException: sun.security.validator.ValidatorException"

Solution:
- Run create-keystore.bat/sh
- Check server.jks exists
- Verify password is correct
```

### Multicast Not Working
```
"No admin notifications received"

Solution:
- Check multicast supported: ipconfig /all (Windows)
- Verify multicast group 230.0.0.1 reachable
- Check firewall/router allows multicast
- Ensure startMulticastListener() called before server sends
```

### Port Already in Use
```
"Address already in use"

Solution:
# Windows
netstat -ano | findstr :8889
taskkill /PID <PID> /F

# Linux
lsof -i :8889
kill -9 <PID>
```

---

## 📚 Learning Resources

### Network Concepts
- TCP/UDP Socket Programming
- Broadcasting (UDP to 255.255.255.255)
- Multicast (UDP to class D addresses)
- SSL/TLS encryption & certificates
- Thread pools & concurrent programming

### Java Topics
- Java NIO & Socket classes
- ExecutorService & ThreadPool
- CompletableFuture for async operations
- JavaFX for GUI
- Object serialization

### Security
- Keystore/Truststore management
- SSL/TLS handshake
- Certificate validation
- Cipher suites

---

## 🤝 Contributing

To add new features:

1. **TCP Chat Server** → Implement `ServerHandler.java`
2. **GUI** → Implement `ChatController.java` + FXML
3. **Persistence** → Implement `RAMStorage.java`
4. **Email** → Implement `EmailService.java`

---

## 📄 License

Educational project - Free to use and modify

---

## 👨‍💻 Author

Network Programming Project
Triển khai tính năng UDP/Multicast/SSL

---

## 🔗 Quick Links

- **Implementation Guide**: `IMPLEMENTATION_GUIDE.md`
- **Completion Report**: `COMPLETION_REPORT.md`

---

## 📝 Project Status

```
✅ UDP Buzz - 100% Complete
✅ Broadcast Discovery - 100% Complete
✅ Multicast Admin - 100% Complete
✅ SSL/TLS - 100% Complete
✅ Demo Applications - 100% Complete
✅ Documentation - 100% Complete

🚀 Ready for:
   - Feature integration with JavaFX GUI
   - TCP Chat Server implementation
   - Production deployment (with production certs)
```

---

**Last Updated:** January 2026
**Status:** ✅ Phase 1 Complete - Advanced Networking Features
