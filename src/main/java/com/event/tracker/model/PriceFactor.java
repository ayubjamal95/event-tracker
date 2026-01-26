package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceFactor {
    private String factor;
    private String impact; // "High", "Medium", "Low", "None"
    private String description;
    private Integer contributionPct; // % contribution to price surge
}
