package com.event.tracker.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketmasterEvent {
    private String name;
    private Dates dates;
    private List<Classification> classifications;
    @JsonProperty("_embedded")
    private Embedded _embedded;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dates {
        private Start start;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Start {
            private String localDate;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classification {
        private Segment segment;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Segment {
            private String name;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedded {
        private List<Venue> venues;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Venue {
            private String name;
        }
    }
}