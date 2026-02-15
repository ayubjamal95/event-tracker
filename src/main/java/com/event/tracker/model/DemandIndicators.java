package com.event.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemandIndicators {
    private int majorEventsCount;
    private int totalExpectedVisitors;
    private double eventImpactScore;
    private String demandLevel;
}
