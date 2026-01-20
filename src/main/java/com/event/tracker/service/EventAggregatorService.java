package com.event.tracker.service;

import com.event.tracker.model.Event;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class EventAggregatorService {

    private final TicketmasterService ticketmasterService;
    public EventAggregatorService(
            TicketmasterService ticketmasterService) {
        this.ticketmasterService = ticketmasterService;
    }

    public List<Event> getAllEvents(String city, String startDate, String endDate) {
        List<Event> allEvents = new ArrayList<>();

        try {
            // Fetch from all sources with parallel execution
            CompletableFuture<List<Event>> tmFuture = CompletableFuture.supplyAsync(
                    () -> ticketmasterService.searchEvents(city, startDate, endDate));

            System.out.println("tmFuture: " + tmFuture.get());

            // Collect results
            allEvents.addAll(tmFuture.get());

            System.out.println("Total events found: " + allEvents.size());

            // Remove duplicates based on name similarity and date
            return deduplicateEvents(allEvents);

        } catch (Exception e) {
            System.err.println("Error aggregating events: " + e.getMessage());
            return allEvents;
        }
    }

    private List<Event> deduplicateEvents(List<Event> events) {
        // Simple deduplication by name and date
        return events.stream()
                .collect(Collectors.toMap(
                        e -> e.getName() + "|" + e.getDate(),
                        e -> e,
                        (e1, e2) -> e1
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }
}