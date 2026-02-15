package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonalityInfo {
    private String season;
    private boolean isPeakSeason;
    private String tourismLevel;
    private double typicalOccupancy;
}