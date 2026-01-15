package com.chat.bot;

import com.chat.grpc.CensorProto;
import com.chat.grpc.CensorServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CensorBotServer {
    private static final int PORT = 50051;
    // Danh sách từ bậy mẫu
    private static final List<String> BAD_WORDS = Arrays.asList("bậy", "tục", "bad", "chửi", "ngu");

    public static void main(String[] args) throws IOException, InterruptedException {
        // Khởi chạy gRPC Server
        Server server = ServerBuilder.forPort(PORT)
                .addService(new CensorServiceImpl())
                .build();

        System.out.println("Censor Bot Server started on port " + PORT);
        server.start();
        server.awaitTermination();
    }

    // Cài đặt logic xử lý của Service
    static class CensorServiceImpl extends CensorServiceGrpc.CensorServiceImplBase {
        @Override
        public void checkProfanity(CensorProto.TextRequest request, StreamObserver<CensorProto.ProfanityResponse> responseObserver) {
            String text = request.getText();
            List<String> detected = new ArrayList<>();
            boolean hasProfanity = false;

            // Kiểm tra xem tin nhắn có chứa từ cấm không (không phân biệt hoa thường)
            for (String badWord : BAD_WORDS) {
                if (text.toLowerCase().contains(badWord.toLowerCase())) {
                    hasProfanity = true;
                    detected.add(badWord);
                }
            }

            // Tạo phản hồi
            CensorProto.ProfanityResponse response = CensorProto.ProfanityResponse.newBuilder()
                    .setHasProfanity(hasProfanity)
                    .addAllDetectedWords(detected)
                    .build();

            // Gửi phản hồi về client (Chat Server)
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}