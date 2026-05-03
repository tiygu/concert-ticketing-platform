package com.concert.ticketing.order.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class OrderCreateRequest {

    @NotNull(message = "演出ID不能为空")
    private Long showId;

    @NotNull(message = "选票ID不能为空")
    private Long ticketId;
}
