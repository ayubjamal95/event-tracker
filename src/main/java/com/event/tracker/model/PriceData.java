package com.event.tracker.model;

import lombok.Data;

import java.util.List;

@Data
public class PriceData {
    private Double avgPrice;
    private Double minPrice;
    private Double maxPrice;
    private List<String> sampleHotels;
}