package com.event.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factor {
    private String factor;
    private String description;
    private double impactPercentage;
    private double weight;
    private String severity;
}
