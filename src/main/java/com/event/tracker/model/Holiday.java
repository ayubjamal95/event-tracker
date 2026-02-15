package com.event.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {
    private String name;
    private LocalDate date;
    private String type; // PUBLIC, BANK, SCHOOL
    private boolean isNational;
}
