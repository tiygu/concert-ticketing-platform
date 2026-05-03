package com.concert.ticketing.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AuditOrderRequest {

    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String reason;
}
