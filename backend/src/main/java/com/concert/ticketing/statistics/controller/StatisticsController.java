package com.concert.ticketing.statistics.controller;

import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.statistics.dto.RevenueReportResponse;
import com.concert.ticketing.statistics.dto.StockStatsResponse;
import com.concert.ticketing.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/api/admin/statistics/revenue")
    public Result<RevenueReportResponse> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        extractCurrentUsername();
        return Result.ok(statisticsService.getRevenueReport(startDate, endDate));
    }

    @GetMapping("/api/admin/statistics/stock")
    public Result<StockStatsResponse> getStockStats(@RequestParam Long showId) {
        extractCurrentUsername();
        return Result.ok(statisticsService.getStockStats(showId));
    }

    private String extractCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
