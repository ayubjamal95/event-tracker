package com.event.tracker.model;

import lombok.Data;

@Data
public class Event {
    private String name;
    private String date;
    private String type;
    private String venue;
}