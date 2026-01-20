package com.event.tracker.model;

import lombok.Data;

import java.util.List;

@Data
public class EventAnalysisResponse {
    private List<Event> events;
    private HotelPrices hotelPrices;
    private Double surgePct;
    private List<Alternative> alternatives;
    private String analysis;
}