package com.event.tracker.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.event.tracker.model.Factor;
import com.event.tracker.model.SurgeCalculation;
import com.event.tracker.service.BaselinePriceService;
import com.event.tracker.service.SurgeCalculationEngine;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class CalculateFinalSurgeTool {

    private final SurgeCalculationEngine surgeEngine;
    private final BaselinePriceService baselinePriceService;
    private final ObjectMapper objectMapper;

    public CalculateFinalSurgeTool(SurgeCalculationEngine surgeEngine,
                                   BaselinePriceService baselinePriceService) {
        this.surgeEngine = surgeEngine;
        this.baselinePriceService = baselinePriceService;
        this.objectMapper = new ObjectMapper();
    }
    @Tool(
            name = "calculate_final_surge",
            description = """
            [HOTEL SURGE ANALYSIS - STEP 3 of 3]

            Calculates deterministic hotel price surge with detailed factor breakdown.
            Provides the WHY behind high hotel prices with quantified impact of each factor.

            PREREQUISITES:
            1. get_analysis_parameters (events/demand data)
            2. trivago-accommodation-search (current market prices)

            REQUIRED INPUTS:
            - analysis_parameters: JSON from get_analysis_parameters
            - current_market_data: Hotel prices from Trivago (average_price required)
            """
    )
    public JsonNode execute(
            @ToolParam(description = "Output from get_analysis_parameters") JsonNode analysisParameters,
            @ToolParam(description = "Trivago hotel prices, must include average_price") JsonNode currentMarketData,
            @ToolParam(description = "Optional baseline price for comparison", required = false) Double baselinePrice,
            @ToolParam(description = "Calculation sensitivity mode: standard/conservative/aggressive", required = false) String calculationMode
    ) {
        try {
            // Validate workflow
            if (!analysisParameters.has("metadata") || !analysisParameters.get("metadata").has("data_sources")) {
                return createWorkflowError();
            }
            if (!currentMarketData.has("average_price")) {
                return createMissingMarketDataError();
            }

            // Determine baseline
            double baseline = (baselinePrice != null) ? baselinePrice :
                    baselinePriceService.estimate(
                            analysisParameters.get("city").asText(),
                            analysisParameters.get("stay_period")
                    );

            double currentAvgPrice = currentMarketData.get("average_price").asDouble();
            String mode = (calculationMode != null) ? calculationMode : "standard";

            // Compute surge
            SurgeCalculation result = surgeEngine.calculate(
                    analysisParameters,
                    currentAvgPrice,
                    baseline,
                    mode
            );

            return buildSurgeResponse(result, baseline, currentAvgPrice, currentMarketData);

        } catch (Exception e) {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", "EXECUTION_ERROR");
            error.put("message", e.getMessage());
            return error;
        }
    }

    private JsonNode createWorkflowError() {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "WORKFLOW_ERROR");
        error.put("message", "Invalid analysis_parameters. Must call get_analysis_parameters first.");

        ArrayNode workflow = objectMapper.createArrayNode();
        workflow.add("1. get_analysis_parameters");
        workflow.add("2. trivago-accommodation-search");
        workflow.add("3. calculate_final_surge (this tool)");
        error.set("required_workflow", workflow);

        return error;
    }

    private JsonNode createMissingMarketDataError() {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "MISSING_MARKET_DATA");
        error.put("message", "Missing Trivago hotel prices. Must provide average_price.");
        return error;
    }

    private JsonNode buildSurgeResponse(SurgeCalculation calc, double baseline, double current, JsonNode marketData) {
        ObjectNode response = objectMapper.createObjectNode();

        // Surge analysis
        ObjectNode surgeAnalysis = objectMapper.createObjectNode();
        surgeAnalysis.put("baseline_price", baseline);
        surgeAnalysis.put("current_average_price", current);
        surgeAnalysis.put("absolute_increase", current - baseline);
        surgeAnalysis.put("surge_percentage", calc.getSurgePercentage());
        surgeAnalysis.put("surge_category", calc.getSurgeCategory());
        surgeAnalysis.put("currency", marketData.has("currency") ? marketData.get("currency").asText() : "EUR");
        response.set("surge_analysis", surgeAnalysis);

        // Contributing factors
        ArrayNode factorsArray = objectMapper.createArrayNode();
        for (Factor factor : calc.getFactors()) {
            ObjectNode factorNode = objectMapper.createObjectNode();
            factorNode.put("factor", factor.getFactor());
            factorNode.put("description", factor.getDescription());
            factorNode.put("impact_percentage", factor.getImpactPercentage());
            factorNode.put("weight", factor.getWeight());
            factorNode.put("severity", factor.getSeverity());
            factorsArray.add(factorNode);
        }
        response.set("contributing_factors", factorsArray);

        // Factor weights
        ObjectNode weights = objectMapper.createObjectNode();
        weights.put("events", calc.getEventWeight());
        weights.put("seasonality", calc.getSeasonalityWeight());
        weights.put("calendar", calc.getCalendarWeight());
        weights.put("demand_supply", calc.getDemandSupplyWeight());
        response.set("factor_weights", weights);

        // Insights
        ObjectNode insights = objectMapper.createObjectNode();
        insights.put("primary_driver", calc.getPrimaryDriver());
        insights.put("confidence_level", calc.getConfidenceLevel());
        insights.put("is_surge_justified", calc.isSurgeJustified());
        insights.put("explanation", calc.getExplanation());
        response.set("insights", insights);

        // Recommendations
        ArrayNode recommendations = objectMapper.createArrayNode();
        for (Map<String, Object> rec : calc.getRecommendations()) {
            ObjectNode recNode = objectMapper.createObjectNode();
            rec.forEach((k, v) -> {
                if (v instanceof String) recNode.put(k, (String) v);
                else if (v instanceof Number) recNode.put(k, ((Number) v).doubleValue());
                else if (v instanceof Boolean) recNode.put(k, (Boolean) v);
            });
            recommendations.add(recNode);
        }
        response.set("recommendations", recommendations);

        // Metadata
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("algorithm_version", "1.0");
        metadata.put("calculation_mode", calc.getMode());
        metadata.put("timestamp", Instant.now().toString());
        response.set("calculation_metadata", metadata);

        return response;
    }
}
