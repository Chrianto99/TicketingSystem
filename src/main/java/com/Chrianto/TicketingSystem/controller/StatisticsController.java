package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.response.EntityShareResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketStatsPointResponse;
import com.Chrianto.TicketingSystem.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketStatsPointResponse>> getTicketStats(
            @RequestParam(defaultValue = "daily") String granularity) {
        List<TicketStatsPointResponse> points = "monthly".equalsIgnoreCase(granularity)
                ? statisticsService.getMonthlyStats()
                : statisticsService.getDailyStats();
        return ResponseEntity.ok(points);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<EntityShareResponse>> getCategoryShares() {
        return ResponseEntity.ok(statisticsService.getCategoryShares());
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<EntityShareResponse>> getSubcategoryShares() {
        return ResponseEntity.ok(statisticsService.getSubcategoryShares());
    }
}
