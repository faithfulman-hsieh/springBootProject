package com.taskmanager.calendar.service;

import com.taskmanager.calendar.dto.CalendarEventDto;

import java.io.IOException;
import java.util.List;

/**
 * 行事曆服務介面 — 抽象 Calendar 操作。
 */
public interface CalendarService {

    List<CalendarEventDto> listEvents(String timeMin, String timeMax) throws IOException;

    CalendarEventDto createEvent(CalendarEventDto dto) throws IOException;

    CalendarEventDto updateEvent(String eventId, CalendarEventDto dto) throws IOException;

    void deleteEvent(String eventId) throws IOException;
}
