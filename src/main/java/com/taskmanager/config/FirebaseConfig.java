package com.taskmanager.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // 1. 優先嘗試從環境變數讀取 (適用於 Render / Docker 部署)
        String base64Config = System.getenv("FIREBASE_JSON_BASE64");

        if (base64Config != null && !base64Config.isEmpty()) {
            try {
                // 解碼 Base64 並轉為 InputStream
                byte[] decodedBytes = Base64.getDecoder().decode(base64Config);
                try (InputStream stream = new ByteArrayInputStream(decodedBytes)) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(stream))
                            .build();
                    return FirebaseApp.initializeApp(options);
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Firebase Base64 環境變數格式錯誤: " + e.getMessage());
            }
        }

        // 2. 如果環境變數沒有，則嘗試讀取本地檔案 (適用於本地開發)
        // 注意：本地開發時，您依然要把 json 檔放在 src/main/resources/ 下，但不要 commit
        try {
            ClassPathResource resource = new ClassPathResource("jproject-push-firebase.json");
            if (resource.exists()) {
                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    return FirebaseApp.initializeApp(options);
                }
            }
        } catch (Exception e) {
            System.err.println("本地 Firebase 設定檔讀取失敗，推播功能可能無法使用: " + e.getMessage());
        }

        // 如果都失敗，回傳 null 或拋出異常 (視您的需求而定，這裡避免崩潰先不拋錯)
        System.err.println("警告：未找到 Firebase 設定 (Env 或 File)，FCM 無法運作。");
        return null;
    }
}