package com.chat.client.network;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpClientUtil {

    /**
     * Upload file lên HTTP Server và nhận về đường dẫn (URL) của file đó.
     *
     * @param file      File cần upload từ máy tính.
     * @param serverUrl Đường dẫn API upload (ví dụ: http://localhost:8080/api/upload).
     * @return String URL của file trên server nếu thành công, hoặc null nếu thất bại.
     */
    public static String uploadFile(File file, String serverUrl) {
        // 1. Kiểm tra file hợp lệ
        if (file == null || !file.exists()) {
            System.err.println("[HttpClientUtil] File không tồn tại hoặc null.");
            return null;
        }

        HttpURLConnection conn = null;
        try {
            // 2. Thiết lập kết nối
            URL url = new URL(serverUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true); // Cho phép gửi dữ liệu (Body)
            conn.setRequestMethod("POST");
            
            // Header quan trọng:
            // - Content-Type: application/octet-stream (Gửi dạng binary thô, dễ xử lý nhất ở server đơn giản)
            // - X-File-Name: Gửi tên file gốc để Server biết giữ lại đuôi file (.jpg, .png...)
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("X-File-Name", file.getName());

            // 3. Gửi dữ liệu file (Streaming)
            try (OutputStream os = conn.getOutputStream();
                 FileInputStream fis = new FileInputStream(file)) {
                
                byte[] buffer = new byte[4096]; // Bộ đệm 4KB
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            // 4. Đọc phản hồi từ Server
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // Code 200
                // Server sẽ trả về URL của ảnh dưới dạng text plain
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    // Trả về chuỗi URL (vd: http://localhost:8080/files/abc.jpg)
                    return result.toString();
                }
            } else {
                System.err.println("[HttpClientUtil] Upload thất bại. Response Code: " + responseCode);
            }

        } catch (IOException e) {
            System.err.println("[HttpClientUtil] Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null; // Trả về null nếu có lỗi
    }
}