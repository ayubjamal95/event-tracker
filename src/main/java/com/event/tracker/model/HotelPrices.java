package com.event.tracker.model;

import lombok.Data;

@Data
public class HotelPrices {
    private PriceData eventDates;
    private PriceData baseline;
}