package com.event.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurgeCalculation {
    private double surgePercentage;
    private double modelSurge;
    private String surgeCategory;
    private String primaryDriver;
    private double confidenceLevel;
    private boolean surgeJustified;
    private String explanation;
    private String mode;
    private List<Factor> factors;
    private Map<String, Double> factorWeights;
    private List<Map<String, Object>> recommendations;

    // Weights
    private double eventWeight;
    private double seasonalityWeight;
    private double calendarWeight;
    private double demandSupplyWeight;
}
