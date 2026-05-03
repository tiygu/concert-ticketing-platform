package com.concert.ticketing.statistics.service;

import com.concert.ticketing.statistics.dto.RevenueReportResponse;
import com.concert.ticketing.statistics.dto.StockStatsResponse;

import java.time.LocalDate;

public interface StatisticsService {

    RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate);

    StockStatsResponse getStockStats(Long showId);
}
