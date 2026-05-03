package com.concert.ticketing.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailItem {

    private Long ticketId;

    private String ticketType;

    private String priceType;

    private BigDecimal price;

    private Integer stock;

    private Integer sold;

    private Integer total;

    private Double soldPercentage;
}
