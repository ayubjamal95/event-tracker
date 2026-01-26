package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceAnalysis {
    private PriceData currentPeriod;
    private PriceData baselineWeekBefore;
    private PriceData baselineMonthBefore;
    private PriceData baselineYearBefore;
    private PriceData neighboringDates;
    private Double surgePct;
    private String confidence; // "High", "Medium", "Low"
}