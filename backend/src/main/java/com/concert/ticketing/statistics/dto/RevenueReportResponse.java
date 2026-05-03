package com.concert.ticketing.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    private Long totalOrders;

    private Long totalTickets;

    private BigDecimal totalRevenue;

    private List<RevenueDetailItem> details;
}
