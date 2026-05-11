package com.taskmanager.calendar.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Google Calendar API 配置 — OAuth2 授權碼流程（Installed App）。
 * <p>
 * 流程：
 * 1. 使用者訪問 /api/calendar/auth → 導向 Google 登入授權
 * 2. 授權後 Google 回呼 /api/calendar/callback → 取得 token
 * 3. 後續 API 呼叫自動帶上使用者的 access token
 */
@Configuration
public class GoogleCalendarConfig {

    private static final String APPLICATION_NAME = "TaskManager-Calendar-PoC";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.calendar.credentials-path:classpath:google-calendar-credentials.json}")
    private Resource credentialsResource;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.redirect-uri:http://localhost:8080/api/calendar/callback}")
    private String redirectUri;

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow()
            throws IOException, GeneralSecurityException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(credentialsResource.getInputStream()));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                Collections.singleton(CalendarScopes.CALENDAR))
                .setCredentialDataStore(
                        StoredCredential.getDefaultDataStore(new MemoryDataStoreFactory()))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    @Bean
    public String googleCalendarId() {
        return calendarId;
    }

    @Bean
    public String googleCalendarRedirectUri() {
        return redirectUri;
    }

    /**
     * 根據已授權的 Credential 建立 Calendar 服務。
     * 這不是單例 Bean — 由 Service 在需要時呼叫。
     */
    public static Calendar buildCalendarService(Credential credential)
            throws GeneralSecurityException, IOException {
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
