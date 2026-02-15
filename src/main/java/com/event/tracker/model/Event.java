package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    private String id;
    private String name;
    private String type;
    private String venue;
    private LocalDate date;
    private LocalTime time;
    private Integer capacity;
    private Integer expectedVisitors;
    private Double distanceKm;
    private String impactLevel;
    private String ticketAvailability;
}