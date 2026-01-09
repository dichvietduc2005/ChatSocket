package com.chat.common.crypto;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Tiện ích để tạo SSLContext cho SSL/TLS Socket.
 * Dùng chung cho cả Server và Client.
 */
public class SSLUtil {

    /**
     * Tạo SSLContext cho Server (cần KeyStore chứa chứng chỉ).
     * 
     * @param keystorePath Đường dẫn tới file .jks
     * @param password     Mật khẩu keystore
     * @return SSLContext đã cấu hình
     */
    public static SSLContext createServerSSLContext(String keystorePath, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext;
    }

    /**
     * Tạo SSLContext cho Client (tin tưởng mọi chứng chỉ - chỉ dùng cho demo).
     * 
     * @return SSLContext đã cấu hình
     */
    public static SSLContext createClientSSLContext() throws Exception {
        // TrustManager chấp nhận mọi chứng chỉ (KHÔNG AN TOÀN - chỉ dùng demo)
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        return sslContext;
    }
}
