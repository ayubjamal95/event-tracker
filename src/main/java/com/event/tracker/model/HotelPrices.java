package com.event.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelPrices {
    private PriceData eventDates;
    private PriceData baseline;
}