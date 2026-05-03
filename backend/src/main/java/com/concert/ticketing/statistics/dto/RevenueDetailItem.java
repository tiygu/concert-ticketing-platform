package com.concert.ticketing.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDetailItem {

    private Long showId;

    private String showName;

    private Long orderCount;

    private Long ticketCount;

    private BigDecimal revenue;
}
