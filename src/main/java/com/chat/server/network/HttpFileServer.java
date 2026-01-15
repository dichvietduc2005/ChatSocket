package com.chat.server.network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class HttpFileServer {
    private static final int PORT = 8080; // Port cho HTTP Server
    private static final String UPLOAD_DIR = "server_files"; // Thư mục lưu file

    public void start() {
        try {   
            // Tạo thư mục lưu trữ nếu chưa có
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Endpoint Upload: POST http://localhost:8888/api/upload
            server.createContext("/api/upload", new UploadHandler());
            
            // Endpoint Xem ảnh: GET http://localhost:8888/files/{filename}
            server.createContext("/files", new StaticFileHandler());

            server.setExecutor(null); // Default executor
            System.out.println("HTTP File Server started on port " + PORT);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Xử lý Upload
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // Lấy tên file gốc từ Header (Client phải gửi header này)
                    String originalName = exchange.getRequestHeaders().getFirst("X-File-Name");
                    if (originalName == null) originalName = "unknown.png";

                    // Tạo tên file duy nhất để tránh trùng
                    String fileName = UUID.randomUUID().toString() + "_" + originalName;
                    Path destination = Paths.get(UPLOAD_DIR, fileName);

                    // Lưu file từ Input Stream
                    InputStream is = exchange.getRequestBody();
                    Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                    is.close();

                    // Trả về URL của ảnh
                    String fileUrl = "http://localhost:" + PORT + "/files/" + fileName;
                    byte[] response = fileUrl.getBytes();

                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, 0);
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    // Xử lý xem ảnh (Download)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            // Lấy tên file từ URL (vd: /files/abc.png -> abc.png)
            String fileName = requestPath.replace("/files/", "");
            File file = new File(UPLOAD_DIR, fileName);

            if (file.exists()) {
                exchange.sendResponseHeaders(200, file.length());
                OutputStream os = exchange.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
}