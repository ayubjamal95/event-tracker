package com.event.tracker;

import com.event.tracker.tools.CalculateFinalSurgeTool;
import com.event.tracker.tools.GetAnalysisParametersTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class EventHotelTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventHotelTrackerApplication.class, args);
	}
	@Bean
	public List<ToolCallback> hotelTools(CalculateFinalSurgeTool calculateFinalSurgeTool,
										 GetAnalysisParametersTool getAnalysisParametersTool) {
		return List.of(ToolCallbacks.from(calculateFinalSurgeTool, getAnalysisParametersTool));
	}

}
