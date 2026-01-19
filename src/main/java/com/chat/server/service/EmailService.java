package com.chat.server.service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailService {

    // --- CẤU HÌNH SMTP GMAIL ---
    // Lưu ý: Bạn cần thay đổi 2 dòng dưới đây thành thông tin thật của bạn
    private static final String SENDER_EMAIL = "trognghia648@gmail.com"; 
    private static final String SENDER_PASSWORD = "medt vntj mwhw yipl"; // Mật khẩu ứng dụng (App Password) - 16 ký tự

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587"; // Port cho TLS

    /**
     * Gửi email cảnh báo khi user offline.
     * Chạy bất đồng bộ (Async) để không chặn luồng chính của Server.
     *
     * @param receiverUsername Tên người nhận (User đang offline)
     * @param senderUsername   Tên người gửi tin nhắn
     * @param messageContent   Nội dung tin nhắn
     */
    public static void sendOfflineNotification(String receiverUsername, String senderUsername, String messageContent) {
        // 1. Giả lập lấy email từ Database dựa vào username
        String receiverEmail = getEmailByUsername(receiverUsername);
        
        if (receiverEmail == null || receiverEmail.isEmpty()) {
            System.out.println("[EmailService] Không tìm thấy email cho user: " + receiverUsername);
            return;
        }

        // 2. Chạy luồng riêng để gửi mail
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[EmailService] Đang gửi mail tới " + receiverEmail + "...");
                
                String subject = "\uD83D\uDD14 Bạn có tin nhắn mới từ " + senderUsername; // Biểu tượng cái chuông
                
                // Nội dung Email dạng HTML
                String htmlBody = "<h3>Xin chào " + receiverUsername + ",</h3>"
                        + "<p>Bạn vừa nhận được tin nhắn từ <b>" + senderUsername + "</b> trong lúc offline.</p>"
                        + "<div style='border-left: 4px solid #007bff; padding-left: 10px; margin: 10px 0; color: #555;'>"
                        + "<i>\"" + messageContent + "\"</i>"
                        + "</div>"
                        + "<p>Vui lòng đăng nhập vào ứng dụng ChatSocket để trả lời.</p>"
                        + "<hr>"
                        + "<small>Đây là tin nhắn tự động, vui lòng không trả lời email này.</small>";

                sendEmail(receiverEmail, subject, htmlBody);
                
                System.out.println("[EmailService] Gửi thành công tới " + receiverEmail);
            } catch (Exception e) {
                System.err.println("[EmailService] Lỗi gửi mail: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Hàm cấu hình và gửi email qua SMTP
     */
    private static void sendEmail(String toEmail, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST); // Tin tưởng chứng chỉ SSL của Gmail

        // Tạo phiên làm việc với Authenticator
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        // Tạo message
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject, "utf-8");
        message.setContent(htmlBody, "text/html; charset=utf-8"); // Gửi dưới dạng HTML utf-8

        // Gửi đi
        Transport.send(message);
    }

    /**
     * Mock Database: Ánh xạ Username -> Email.
     * Trong thực tế, bạn sẽ query SQL ở đây.
     */
    private static String getEmailByUsername(String username) {
        return "dvd192005@gmail.com"; 
    }
}