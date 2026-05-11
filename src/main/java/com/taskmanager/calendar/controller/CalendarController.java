package com.taskmanager.calendar.controller;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.taskmanager.calendar.dto.CalendarEventDto;
import com.taskmanager.calendar.service.GoogleCalendarService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Google Calendar REST API — PoC（OAuth2 授權碼流程）。
 * <p>
 * 使用流程：
 * 1. GET /api/calendar/auth → 取得 Google 授權 URL，前端導向此 URL
 * 2. 使用者在 Google 頁面授權後，回呼 /api/calendar/callback?code=xxx
 * 3. 授權完成後即可使用 CRUD API
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final GoogleCalendarService calendarService;
    private final GoogleAuthorizationCodeFlow authFlow;
    private final String redirectUri;

    public CalendarController(
            GoogleCalendarService calendarService,
            GoogleAuthorizationCodeFlow authFlow,
            @Qualifier("googleCalendarRedirectUri") String redirectUri) {
        this.calendarService = calendarService;
        this.authFlow = authFlow;
        this.redirectUri = redirectUri;
    }

    // ─── OAuth2 授權 ───

    /**
     * 取得 Google 授權 URL 或重導向至 Google 登入。
     * 前端可以用回傳的 authUrl 開啟新分頁。
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, String>> getAuthUrl() {
        String authUrl = authFlow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * Google OAuth2 回呼端點。
     * Google 授權後會帶 code 參數回呼此 URL。
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code) {
        try {
            TokenResponse tokenResponse = authFlow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            authFlow.createAndStoreCredential(tokenResponse, GoogleCalendarService.DEFAULT_USER_ID);

            // 回傳一個簡單的 HTML 頁面，自動關閉視窗並通知主頁面
            String html = """
                <!DOCTYPE html>
                <html>
                <head><title>授權成功</title></head>
                <body style="display:flex;justify-content:center;align-items:center;height:100vh;
                             font-family:Inter,sans-serif;background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;">
                  <div style="text-align:center;background:rgba(255,255,255,0.15);padding:40px 60px;border-radius:20px;backdrop-filter:blur(10px);">
                    <h1 style="margin:0 0 8px;">✅ 授權成功！</h1>
                    <p style="opacity:0.9;">Google Calendar 已連結，可以關閉此視窗。</p>
                    <script>
                      if (window.opener) {
                        window.opener.postMessage({ type: 'GOOGLE_CALENDAR_AUTH_SUCCESS' }, '*');
                      }
                      setTimeout(() => window.close(), 2000);
                    </script>
                  </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(html);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("授權失敗：" + e.getMessage());
        }
    }

    /**
     * 檢查是否已完成 Google 授權。
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus() {
        boolean authorized = calendarService.isAuthorized();
        return ResponseEntity.ok(Map.of(
                "authorized", authorized,
                "message", authorized ? "已連結 Google Calendar" : "尚未授權，請先訪問 /api/calendar/auth"
        ));
    }

    // ─── 事件 CRUD ───

    /**
     * 查詢指定時間區間的事件。
     */
    @GetMapping("/events")
    public ResponseEntity<?> listEvents(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            List<CalendarEventDto> events = calendarService.listEvents(start, end);
            return ResponseEntity.ok(events);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage(), "authorized", false));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "載入事件失敗：" + e.getMessage()));
        }
    }

    /**
     * 新增行事曆事件。
     */
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody CalendarEventDto dto) {
        try {
            CalendarEventDto created = calendarService.createEvent(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage(), "authorized", false));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "建立事件失敗：" + e.getMessage()));
        }
    }

    /**
     * 更新行事曆事件。
     */
    @PutMapping("/events/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String eventId,
            @RequestBody CalendarEventDto dto) {
        try {
            CalendarEventDto updated = calendarService.updateEvent(eventId, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage(), "authorized", false));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新事件失敗：" + e.getMessage()));
        }
    }

    /**
     * 刪除行事曆事件。
     */
    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable String eventId) {
        try {
            calendarService.deleteEvent(eventId);
            return ResponseEntity.ok(Map.of("message", "Event deleted", "eventId", eventId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage(), "authorized", false));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "刪除事件失敗：" + e.getMessage()));
        }
    }
}
