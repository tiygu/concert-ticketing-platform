package com.concert.ticketing.vip.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class VipBookingAuditRequest {

    @NotBlank(message = "审核状态不能为空")
    private String auditStatus;

    @Size(max = 2000, message = "回复不能超过2000个字符")
    private String adminReply;
}
