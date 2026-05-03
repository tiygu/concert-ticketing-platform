package com.concert.ticketing.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
