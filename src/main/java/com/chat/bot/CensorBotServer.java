package com.chat.bot;

import com.chat.grpc.CensorProto;
import com.chat.grpc.CensorServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CensorBotServer {
    private static final int PORT = 50051;
    // Danh sách từ cấm sẽ được nạp từ file
    private static final List<String> BAD_WORDS = Collections.synchronizedList(new ArrayList<>());
    
    // Tên file chứa danh sách cấm (đặt ở thư mục gốc dự án)
    private static final String BANNED_FILE_PATH = "banned_words.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        // 1. Nạp danh sách từ cấm từ file
        loadBadWords();

        // 2. Khởi chạy gRPC Server
        Server server = ServerBuilder.forPort(PORT)
                .addService(new CensorServiceImpl())
                .build();

        System.out.println("Censor Bot Server started on port " + PORT);
        System.out.println("Đã nạp " + BAD_WORDS.size() + " từ khoá cấm vào bộ nhớ.");
        
        server.start();
        server.awaitTermination();
    }

    // Hàm đọc file và xử lý định dạng JSON: "tu_khoa": 1,
    private static void loadBadWords() {
        System.out.println("Đang đọc file từ cấm: " + BANNED_FILE_PATH);
        try (BufferedReader br = new BufferedReader(new FileReader(BANNED_FILE_PATH, StandardCharsets.UTF_8))) {
            String line;
            
            // [CẬP NHẬT] Regex mới: Bắt nội dung trong cả dấu nháy kép (") và nháy đơn (')
            // Giải thích: ["'] nghĩa là " hoặc '. ([^"']+) nghĩa là lấy nội dung bên trong không chứa dấu nháy.
            Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
            
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) { // Dùng while để tìm tất cả các từ trong 1 dòng
                    String word = matcher.group(1); // Lấy từ khóa
                    
                    if (word.length() < 2) continue; // Bỏ qua từ quá ngắn (1 ký tự) để tránh lọc nhầm
                    
                    // 1. Thêm từ gốc
                    BAD_WORDS.add(word);
                    
                    // 2. Thêm phiên bản thay thế dấu gạch dưới (nếu có)
                    if (word.contains("_")) {
                        BAD_WORDS.add(word.replace("_", " "));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ CẢNH BÁO: Không tìm thấy file " + BANNED_FILE_PATH);
            // Thêm từ mặc định
            BAD_WORDS.add("dm");
            BAD_WORDS.add("chui");
        }
    }

    static class CensorServiceImpl extends CensorServiceGrpc.CensorServiceImplBase {
        @Override
        public void checkProfanity(CensorProto.TextRequest request, StreamObserver<CensorProto.ProfanityResponse> responseObserver) {
            String originalText = request.getText();
            String lowerCaseText = originalText.toLowerCase();
            List<String> detected = new ArrayList<>();
            boolean hasProfanity = false;

            // Duyệt qua danh sách (Đã nạp từ file)
            // Lưu ý: Với danh sách lớn, dùng loop có thể hơi chậm. 
            // Nếu muốn nhanh hơn cần dùng thuật toán Aho-Corasick, nhưng loop vẫn ổn với < 5000 từ.
            for (String badWord : BAD_WORDS) {
                if (lowerCaseText.contains(badWord.toLowerCase())) {
                    hasProfanity = true;
                    detected.add(badWord);
                    // Có thể break ngay nếu chỉ cần biết có/không
                    // break; 
                }
            }

            CensorProto.ProfanityResponse response = CensorProto.ProfanityResponse.newBuilder()
                    .setHasProfanity(hasProfanity)
                    .addAllDetectedWords(detected)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}