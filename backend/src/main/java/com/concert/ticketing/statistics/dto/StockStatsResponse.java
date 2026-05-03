package com.concert.ticketing.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatsResponse {

    private Long showId;

    private String showName;

    private Integer totalStock;

    private Integer totalSold;

    private Integer totalCapacity;

    private Double soldPercentage;
    private List<StockDetailItem> details;
}
