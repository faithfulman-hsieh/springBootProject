package com.taskmanager.calendar.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.taskmanager.calendar.config.GoogleCalendarConfig;
import com.taskmanager.calendar.dto.CalendarEventDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Calendar API 封裝服務 — OAuth2 版本。
 * <p>
 * 需要使用者先透過 /api/calendar/auth 完成 Google 授權後才能使用。
 * 授權後的 Credential 存在 GoogleAuthorizationCodeFlow 的 DataStore 中。
 */
@Service
public class GoogleCalendarService implements CalendarService {

    /** PoC 簡化：使用固定的 user ID 儲存 token */
    public static final String DEFAULT_USER_ID = "poc-user";

    private final GoogleAuthorizationCodeFlow authFlow;
    private final String calendarId;

    public GoogleCalendarService(
            GoogleAuthorizationCodeFlow authFlow,
            @Qualifier("googleCalendarId") String calendarId) {
        this.authFlow = authFlow;
        this.calendarId = calendarId;
    }

    /**
     * 檢查使用者是否已完成 Google 授權。
     */
    public boolean isAuthorized() {
        try {
            Credential credential = authFlow.loadCredential(DEFAULT_USER_ID);
            return credential != null && credential.getAccessToken() != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 取得目前的 Calendar 服務實例。
     * @throws IllegalStateException 如果尚未授權
     */
    private Calendar getCalendar() throws IOException {
        Credential credential = authFlow.loadCredential(DEFAULT_USER_ID);
        if (credential == null || credential.getAccessToken() == null) {
            throw new IllegalStateException("尚未完成 Google 授權，請先訪問 /api/calendar/auth");
        }
        try {
            return GoogleCalendarConfig.buildCalendarService(credential);
        } catch (GeneralSecurityException e) {
            throw new IOException("無法建立 Calendar 服務", e);
        }
    }

    @Override
    public List<CalendarEventDto> listEvents(String timeMin, String timeMax) throws IOException {
        Calendar calendar = getCalendar();
        Events events = calendar.events().list(calendarId)
                .setTimeMin(new DateTime(timeMin))
                .setTimeMax(new DateTime(timeMax))
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .execute();

        List<Event> items = events.getItems();
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CalendarEventDto createEvent(CalendarEventDto dto) throws IOException {
        Calendar calendar = getCalendar();
        Event event = toGoogleEvent(dto);
        Event created = calendar.events().insert(calendarId, event)
                .setSendUpdates("all")
                .execute();
        return toDto(created);
    }

    @Override
    public CalendarEventDto updateEvent(String eventId, CalendarEventDto dto) throws IOException {
        Calendar calendar = getCalendar();
        Event event = toGoogleEvent(dto);
        Event updated = calendar.events().update(calendarId, eventId, event)
                .setSendUpdates("all")
                .execute();
        return toDto(updated);
    }

    @Override
    public void deleteEvent(String eventId) throws IOException {
        Calendar calendar = getCalendar();
        calendar.events().delete(calendarId, eventId).execute();
    }

    // ─── 內部轉換方法 ───

    private CalendarEventDto toDto(Event event) {
        CalendarEventDto dto = new CalendarEventDto();
        dto.setId(event.getId());
        dto.setSummary(event.getSummary());
        dto.setDescription(event.getDescription());
        dto.setLocation(event.getLocation());
        dto.setColorId(event.getColorId());

        if (event.getStart() != null) {
            if (event.getStart().getDate() != null) {
                dto.setAllDay(true);
                dto.setStartDateTime(event.getStart().getDate().toStringRfc3339());
            } else if (event.getStart().getDateTime() != null) {
                dto.setAllDay(false);
                dto.setStartDateTime(event.getStart().getDateTime().toStringRfc3339());
            }
        }

        if (event.getEnd() != null) {
            if (event.getEnd().getDate() != null) {
                dto.setEndDateTime(event.getEnd().getDate().toStringRfc3339());
            } else if (event.getEnd().getDateTime() != null) {
                dto.setEndDateTime(event.getEnd().getDateTime().toStringRfc3339());
            }
        }

        if (event.getAttendees() != null) {
            List<String> attendees = event.getAttendees().stream()
                    .map(EventAttendee::getEmail)
                    .collect(Collectors.toList());
            dto.setAttendees(attendees);
        }

        return dto;
    }

    private Event toGoogleEvent(CalendarEventDto dto) {
        Event event = new Event();
        event.setSummary(dto.getSummary());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setColorId(dto.getColorId());

        if (dto.isAllDay()) {
            event.setStart(new EventDateTime().setDate(new DateTime(dto.getStartDateTime())));
            event.setEnd(new EventDateTime().setDate(new DateTime(dto.getEndDateTime())));
        } else {
            event.setStart(new EventDateTime().setDateTime(new DateTime(dto.getStartDateTime())));
            event.setEnd(new EventDateTime().setDateTime(new DateTime(dto.getEndDateTime())));
        }

        if (dto.getAttendees() != null && !dto.getAttendees().isEmpty()) {
            List<EventAttendee> attendees = dto.getAttendees().stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .collect(Collectors.toList());
            event.setAttendees(attendees);
        }

        return event;
    }
}
