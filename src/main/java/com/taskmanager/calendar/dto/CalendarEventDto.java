package com.taskmanager.calendar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行事曆事件 DTO — 前後端資料交換格式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventDto {

    /** Google Calendar Event ID（新增時為 null，回傳時帶值） */
    private String id;

    /** 事件標題 */
    private String summary;

    /** 事件描述 */
    private String description;

    /** 地點 */
    private String location;

    /** 開始時間（ISO-8601，例如 2026-05-10T10:00:00） */
    private String startDateTime;

    /** 結束時間（ISO-8601，例如 2026-05-10T11:00:00） */
    private String endDateTime;

    /** 是否為全天事件 */
    private boolean allDay;

    /** 事件顏色 ID（Google Calendar 內建 1~11） */
    private String colorId;

    /** 參與者 Email 列表 */
    private java.util.List<String> attendees;
}
