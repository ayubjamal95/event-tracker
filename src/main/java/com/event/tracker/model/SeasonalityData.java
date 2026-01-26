package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonalityData {
    private String season;
    private Boolean isHoliday;
    private Boolean isPeakSeason;
    private Boolean isWeekend;
    private List<String> holidays;
}