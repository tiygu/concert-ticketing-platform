package com.concert.ticketing.vip.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class VipBookingCreateRequest {

    @NotNull(message = "套餐ID不能为空")
    private Long packageId;

    @NotNull(message = "使用日期不能为空")
    private LocalDate useDate;
}
