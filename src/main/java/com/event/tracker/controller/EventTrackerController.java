package com.event.tracker.controller;

import com.event.tracker.model.EventAnalysisRequest;
import com.event.tracker.model.EnhancedAnalysisResponse;
import com.event.tracker.service.EnhancedPriceAnalysisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventTrackerController {

    private final EnhancedPriceAnalysisService analysisService;

    public EventTrackerController(EnhancedPriceAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("request", new EventAnalysisRequest());
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@ModelAttribute EventAnalysisRequest request, Model model) {
        try {
            EnhancedAnalysisResponse response = analysisService.analyzeWithMultipleFactors(request);
            model.addAttribute("request", request);
            model.addAttribute("results", response);
            return "index";
        } catch (Exception e) {
            model.addAttribute("error", "Analysis failed: " + e.getMessage());
            model.addAttribute("request", request);
            e.printStackTrace();
            return "index";
        }
    }
}
