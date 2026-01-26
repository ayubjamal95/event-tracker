package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventImpactScore {
    private Integer score; // 0-100
    private String confidence; // "High", "Medium", "Low"
    private List<String> evidencePoints;
    private List<String> counterPoints;
}