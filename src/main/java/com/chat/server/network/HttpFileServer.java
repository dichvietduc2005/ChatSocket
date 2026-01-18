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

public class HttpFileServer implements Runnable { // Thêm Runnable để chạy cùng ServerMain
    private static final int PORT = 8080; 
    private static final String UPLOAD_DIR = "server_files"; 

    @Override
    public void run() {
        start();
    }

    public void start() {
        try {   
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Endpoint để Client upload ảnh
            server.createContext("/upload", new UploadHandler());
            
            // Endpoint để Client tải/xem ảnh (Đổi thành /files/ cho khớp link trả về)
            server.createContext("/files/", new StaticFileHandler());

            server.setExecutor(null); 
            System.out.println("HTTP File Server started on port " + PORT);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String originalName = exchange.getRequestHeaders().getFirst("X-File-Name");
                    if (originalName == null) originalName = "unknown_file";

                    // [FIX LỖI QUAN TRỌNG] 
                    // Làm sạch tên file: Chỉ giữ lại chữ cái không dấu, số, dấu chấm, gạch ngang.
                    // Các ký tự tiếng Việt có dấu hoặc ký tự lạ (như ?) sẽ bị đổi thành "_"
                    // Regex: [^a-zA-Z0-9.-] nghĩa là "những ký tự KHÔNG phải là chữ, số, chấm, gạch ngang"
                    String safeName = originalName.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
                    
                    // Thêm UUID ngắn để tránh trùng file
                    String fileName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
                    
                    Path destination = Paths.get(UPLOAD_DIR, fileName);

                    try (InputStream is = exchange.getRequestBody()) {
                        Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
                    }

                    // Trả về URL
                    String fileUrl = "http://localhost:" + PORT + "/files/" + fileName;
                    byte[] response = fileUrl.getBytes();

                    exchange.sendResponseHeaders(200, response.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                    System.out.println("[HttpServer] File uploaded: " + fileName);
                    
                } catch (Exception e) {
                    e.printStackTrace(); // In lỗi ra để debug nếu có
                    exchange.sendResponseHeaders(500, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            // Lấy tên file sau /files/
            String fileName = requestPath.substring(requestPath.indexOf("/files/") + 7);
            File file = new File(UPLOAD_DIR, fileName);

            if (file.exists() && !file.isDirectory()) {
                // [QUAN TRỌNG] Header ép buộc trình duyệt tải file về thay vì mở xem
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                
                // Thiết lập Content-Type tự động
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) contentType = "application/octet-stream";
                exchange.getResponseHeaders().set("Content-Type", contentType);

                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }
            } else {
                String response = "File not found";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}