package com.event.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalendarInfo {
    private boolean isWeekend;
    private boolean isHoliday;
    private boolean isLongWeekend;
    private boolean isSchoolHoliday;
    private List<Holiday> holidays;
}
