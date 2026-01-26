package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnhancedAnalysisResponse {
    private List<Event> events;
    private PriceAnalysis priceAnalysis;
    private List<PriceFactor> contributingFactors;
    private EventImpactScore eventImpactScore;
    private List<Alternative> alternatives;
    private OccupancyData occupancyData;
    private String analysis;
}
