# Hướng Dẫn Test Dự Án ChatSocket

Hướng dẫn chi tiết để test tất cả các tính năng đã triển khai.

---

## 📋 Mục Lục

1. [Chuẩn Bị](#chuẩn-bị)
2. [Test TCP Chat (1-1 & Nhóm)](#test-tcp-chat)
3. [Test UDP Buzz](#test-udp-buzz)
4. [Test Broadcast Discovery](#test-broadcast-discovery)
5. [Test Multicast Admin](#test-multicast-admin)
6. [Test SSL/TLS](#test-ssltls)
7. [Test với Wireshark](#test-với-wireshark)
8. [Test Multi-Client](#test-multi-client)

---

## 🔧 Chuẩn Bị

### 1. Kiểm tra Maven đã cài đặt:

```powershell
cd D:\LapTrinhMang\ChatSocket
$env:PATH="$env:LOCALAPPDATA\maven\bin;$env:PATH"
mvn --version
```

### 2. Kiểm tra keystore đã tạo:

```powershell
dir server.jks
```

Nếu chưa có, chạy:
```powershell
.\create-keystore.ps1
```

### 3. Build project:

```powershell
mvn clean compile
```

---

## 💬 Test TCP Chat (1-1 & Nhóm)

### Mục đích: Test chat văn bản qua TCP, đảm bảo tin nhắn không bị mất, thứ tự đúng.

### Bước 1: Chạy Server

**Terminal 1:**
```powershell
cd D:\LapTrinhMang\ChatSocket
$env:PATH="$env:LOCALAPPDATA\maven\bin;$env:PATH"
mvn exec:java "-Dexec.mainClass=com.chat.server.ServerMain"
```

**Kỳ vọng output:**
```
>>> ĐANG KHỞI ĐỘNG HỆ THỐNG CHAT SERVER ĐA DỊCH VỤ <<<
[Service] Chat Server is running on port 8888
[System] Waiting for clients...
```

### Bước 2: Chạy Client 1

**Terminal 2:**
```powershell
cd D:\LapTrinhMang\ChatSocket
$env:PATH="$env:LOCALAPPDATA\maven\bin;$env:PATH"
mvn javafx:run
```

Hoặc nếu có JavaFX client:
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.client.ClientMain"
```

### Bước 3: Chạy Client 2

**Terminal 3:** (Mở terminal mới)
```powershell
cd D:\LapTrinhMang\ChatSocket
$env:PATH="$env:LOCALAPPDATA\maven\bin;$env:PATH"
mvn javafx:run
```

### Bước 4: Test Chat Nhóm

1. **Client 1:** Gửi message "Hello everyone!"
2. **Client 2:** Phải nhận được message từ Client 1
3. **Client 2:** Gửi message "Hi there!"
4. **Client 1:** Phải nhận được message từ Client 2

**Kết quả mong đợi:**
- ✅ Tất cả clients nhận được tin nhắn nhóm
- ✅ Thứ tự tin nhắn đúng
- ✅ Không mất tin nhắn

### Bước 5: Test Chat 1-1 (nếu có)

1. Chọn user trong danh sách
2. Gửi message riêng
3. Chỉ user đó nhận được

---

## 📳 Test UDP Buzz

### Mục đích: Test tính năng rung cửa sổ khi nhận BUZZ.

### Bước 1: Chạy Server

**Terminal 1:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.server.ServerMain"
```

### Bước 2: Chạy 2 Clients

**Terminal 2 & 3:**
```powershell
mvn javafx:run
```

### Bước 3: Test BUZZ

1. **Client 1:** Chọn Client 2 trong danh sách
2. **Client 1:** Click nút "BUZZ" hoặc gửi lệnh buzz
3. **Client 2:** 
   - ✅ Cửa sổ rung lên (10 lần, 5px amplitude)
   - ✅ Phát âm thanh beep (800Hz, 200ms)

**Kết quả mong đợi:**
- ✅ Cửa sổ Client 2 rung lên
- ✅ Có âm thanh beep
- ✅ Không ảnh hưởng đến chat TCP

### Lưu ý:
- UDP Buzz hoạt động độc lập với TCP Chat
- Mất gói tin UDP không ảnh hưởng (best-effort)
- Cần biết IP của client đích

---

## 🔍 Test Broadcast Discovery

### Mục đích: Client tự động tìm Server trên mạng LAN.

### Bước 1: Chạy Server (có Discovery)

**Terminal 1:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoServer"
```

**Kỳ vọng output:**
```
[1] Starting UDP Discovery Server...
UDP Discovery Server started on port 9999
Server IP: 192.168.1.198
✓ UDP Discovery started on port 9999
```

### Bước 2: Test Discovery từ Client

**Terminal 2:**
```powershell
# Chạy client và test discovery
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoClient"
```

Hoặc trong code JavaFX client:
```java
// Tự động discover server
String serverIP = UdpDiscovery.discoverServer().get();
```

**Kết quả mong đợi:**
- ✅ Client tự động tìm thấy Server IP
- ✅ Tự động điền vào ô Server Address
- ✅ Không cần nhập IP thủ công

### Test thủ công:

**Terminal 2:**
```powershell
# Tạo file test-discovery.ps1
$discoveryPort = 9999
$socket = New-Object System.Net.Sockets.UdpClient
$socket.Client.ReceiveTimeout = 3000

$request = [System.Text.Encoding]::ASCII.GetBytes("WHERE_IS_SERVER?")
$endpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Broadcast, $discoveryPort)
$socket.Send($request, $request.Length, $endpoint) | Out-Null

$response = $socket.Receive([ref]$endpoint)
$serverIP = [System.Text.Encoding]::ASCII.GetString($response)
Write-Host "Found server at: $serverIP"

$socket.Close()
```

**Kết quả mong đợi:**
```
Found server at: 192.168.1.198:8888
```

---

## 📢 Test Multicast Admin

### Mục đích: Server gửi thông báo admin đến tất cả clients qua Multicast.

### Bước 1: Chạy Server

**Terminal 1:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoServer"
```

**Kỳ vọng output:**
```
[2] Starting Multicast Admin Server...
Multicast Admin Server started
Multicast address: 230.0.0.1:9997
Type messages to broadcast to all clients (type 'exit' to stop):
```

### Bước 2: Chạy 2+ Clients

**Terminal 2 & 3:**
```powershell
mvn javafx:run
```

### Bước 3: Gửi Admin Notification

**Terminal 1 (Server console):**
```
Server sẽ bảo trì vào 22:00 tối nay
```

**Kết quả mong đợi:**
- ✅ Tất cả clients nhận được thông báo
- ✅ Hiển thị trong notification area
- ✅ Format: "🔔 ADMIN: Server sẽ bảo trì..."

### Test thủ công với PowerShell:

**Terminal 2:**
```powershell
$multicastIP = [System.Net.IPAddress]::Parse("230.0.0.1")
$multicastPort = 9997
$socket = New-Object System.Net.Sockets.UdpClient
$socket.JoinMulticastGroup($multicastIP)

$endpoint = New-Object System.Net.IPEndPoint($multicastIP, $multicastPort)
$buffer = New-Object byte[] 1024

Write-Host "Listening for multicast messages..."
$data = $socket.Receive([ref]$endpoint)
$message = [System.Text.Encoding]::ASCII.GetString($data, 0, $data.Length)
Write-Host "Received: $message"

$socket.Close()
```

---

## 🔒 Test SSL/TLS

### Mục đích: Test mã hóa traffic, đảm bảo không thể đọc được plaintext.

### Bước 1: Kiểm tra keystore

```powershell
dir server.jks
```

Nếu chưa có:
```powershell
.\create-keystore.ps1
```

### Bước 2: Chạy SSL Server

**Terminal 1:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoServer"
```

**Kỳ vọng output:**
```
[3] Starting SSL/TLS Server...
✓ SSL/TLS Server started on port 8889
```

### Bước 3: Chạy SSL Client

**Terminal 2:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoClient"
```

**Kỳ vọng output:**
```
[2] Connecting to server via SSL...
✓ Connected securely with cipher: TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

### Bước 4: Gửi Message

**Terminal 2 (Client):**
```
msg This is a secret message
```

### Bước 5: Test với Wireshark

Xem phần [Test với Wireshark](#test-với-wireshark) bên dưới.

**Kết quả mong đợi:**
- ✅ Thấy TLS Handshake (Client Hello, Server Hello, Certificate)
- ✅ Thấy "Application Data (Encrypted)"
- ✅ Không thể đọc được nội dung message

---

## 📊 Test với Wireshark

### Mục đích: Xác minh traffic có được mã hóa hay không.

### Bước 1: Cài đặt Wireshark

1. Download: https://www.wireshark.org/download.html
2. Cài đặt (chọn Npcap khi được hỏi)

### Bước 2: Chạy Server & Client

**Terminal 1:** SSL Server
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoServer"
```

**Terminal 2:** SSL Client
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.demo.DemoClient"
```

### Bước 3: Bắt đầu Capture trong Wireshark

1. Mở Wireshark
2. Chọn interface:
   - **Localhost:** "Adapter for loopback traffic capture"
   - **LAN:** Chọn adapter tương ứng
3. Click "Start capturing packets" (icon cá mập xanh)

### Bước 4: Áp dụng Filter

Trong ô filter, gõ:
```
tcp.port == 8889
```

Nhấn Enter.

### Bước 5: Tạo Traffic

**Terminal 2 (Client):**
```
msg This message should be encrypted
```

### Bước 6: Phân tích Kết quả

#### ✅ Nếu SSL hoạt động đúng:

Bạn sẽ thấy:

1. **TLS Handshake:**
   ```
   Client Hello          (TLSv1.2)
   Server Hello          (TLSv1.2)
   Certificate           (TLSv1.2)
   Server Hello Done     (TLSv1.2)
   Client Key Exchange   (TLSv1.2)
   Change Cipher Spec   (TLSv1.2)
   Encrypted Handshake   (TLSv1.2)
   ```

2. **Application Data (Encrypted):**
   ```
   Application Data      (TLSv1.2) - Encrypted
   ```

3. **Click vào packet "Application Data":**
   - Mở rộng: "Transport Layer Security" → "TLSv1.2 Record Layer"
   - Thấy: "Encrypted Application Data"
   - **Không thể đọc được nội dung**

#### ❌ Nếu chưa mã hóa (TCP thường):

Bạn sẽ thấy:
- Protocol: TCP (không phải TLS)
- Có thể đọc được plaintext trong "Follow TCP Stream"

### Bước 7: So sánh TCP thường vs SSL

#### Test TCP thường (Port 8888):

1. Chạy `ServerMain` (port 8888)
2. Filter: `tcp.port == 8888`
3. Gửi message
4. **Kết quả:** Có thể đọc được plaintext

#### Test SSL/TLS (Port 8889):

1. Chạy `DemoServer` (port 8889)
2. Filter: `tcp.port == 8889`
3. Gửi message
4. **Kết quả:** Chỉ thấy encrypted data

### Các Filter hữu ích:

```
tcp.port == 8889              # Chỉ xem port SSL
tcp.port == 8888              # Chỉ xem port TCP thường
ssl                            # Tất cả SSL/TLS traffic
ssl.handshake.type == 1        # Client Hello
ssl.handshake.type == 2        # Server Hello
ssl.record.content_type == 23  # Application Data
```

---

## 👥 Test Multi-Client

### Mục đích: Test server xử lý nhiều clients đồng thời.

### Bước 1: Chạy Server

**Terminal 1:**
```powershell
mvn exec:java "-Dexec.mainClass=com.chat.server.ServerMain"
```

### Bước 2: Chạy 5+ Clients

**Terminal 2-6:**
```powershell
mvn javafx:run
```

### Bước 3: Test Chat Nhóm

1. **Client 1:** Gửi "Hello everyone!"
2. **Tất cả clients khác:** Phải nhận được message
3. **Client 2:** Gửi "Hi!"
4. **Tất cả clients khác:** Phải nhận được message

**Kết quả mong đợi:**
- ✅ Server xử lý được nhiều clients đồng thời
- ✅ Không bị block khi có nhiều clients
- ✅ Tất cả clients nhận được tin nhắn nhóm

### Bước 4: Test Performance

1. Gửi nhiều messages liên tiếp
2. Kiểm tra không bị mất message
3. Kiểm tra thứ tự message đúng

---

## ✅ Checklist Test Tổng Hợp

### TCP Chat:
- [ ] Server khởi động thành công
- [ ] Client kết nối được
- [ ] Chat nhóm hoạt động
- [ ] Chat 1-1 hoạt động (nếu có)
- [ ] Không mất message
- [ ] Thứ tự message đúng

### UDP Buzz:
- [ ] UDP listener khởi động
- [ ] Gửi BUZZ thành công
- [ ] Cửa sổ rung lên
- [ ] Có âm thanh beep

### Broadcast Discovery:
- [ ] Discovery server khởi động
- [ ] Client tìm thấy server
- [ ] Tự động điền IP

### Multicast Admin:
- [ ] Multicast server khởi động
- [ ] Gửi notification thành công
- [ ] Tất cả clients nhận được

### SSL/TLS:
- [ ] Keystore đã tạo
- [ ] SSL server khởi động
- [ ] Client kết nối SSL thành công
- [ ] Wireshark thấy TLS handshake
- [ ] Wireshark thấy encrypted data
- [ ] Không thể đọc được plaintext

### Multi-Client:
- [ ] Server xử lý được nhiều clients
- [ ] Không bị block
- [ ] Tất cả clients nhận được message

---

## 🐛 Troubleshooting

### Port đã được sử dụng:

```powershell
# Tìm process đang dùng port
netstat -ano | findstr ":8888 :8889 :9999"

# Kill process (thay PID bằng số thực tế)
taskkill /F /PID <PID>
```

### Keystore không tìm thấy:

```powershell
# Tạo lại keystore
.\create-keystore.ps1
```

### Maven không tìm thấy:

```powershell
# Thêm Maven vào PATH tạm thời
$env:PATH="$env:LOCALAPPDATA\maven\bin;$env:PATH"
```

### Wireshark không thấy traffic:

1. Kiểm tra filter: `tcp.port == 8889`
2. Kiểm tra interface: Chọn đúng network adapter
3. Kiểm tra firewall: Tắt tạm thời để test

---

## 📝 Ghi Chú

- Tất cả tests nên chạy trên cùng một máy (localhost) trước
- Sau đó test trên mạng LAN để kiểm tra network
- Wireshark chỉ cần cho test SSL/TLS
- Các tính năng khác có thể test bằng console output

---

## 🎯 Kết Quả Mong Đợi

Sau khi test xong, bạn sẽ có:

1. ✅ TCP Chat hoạt động ổn định
2. ✅ UDP Buzz rung cửa sổ và phát âm thanh
3. ✅ Broadcast Discovery tự động tìm server
4. ✅ Multicast Admin gửi thông báo đến tất cả clients
5. ✅ SSL/TLS mã hóa traffic (verified bằng Wireshark)
6. ✅ Multi-Client xử lý nhiều clients đồng thời

---

**Chúc bạn test thành công! 🚀**
