package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OccupancyData {
    private Integer availableHotels;
    private Integer totalHotels;
    private Double occupancyRate;
    private String demandLevel; // "Very High", "High", "Normal", "Low"
}