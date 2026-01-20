package com.event.tracker.model;

import lombok.Data;

@Data
public class EventAnalysisRequest {
    private String city;
    private String checkIn;
    private String checkOut;
}
