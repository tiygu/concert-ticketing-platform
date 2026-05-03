package com.concert.ticketing.vip.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class VipPackageCreateRequest {

    @NotBlank(message = "VIP套餐名称不能为空")
    @Size(max = 100, message = "VIP套餐名称不能超过100个字符")
    private String packageName;

    @Size(max = 2000, message = "权益说明不能超过2000个字符")
    private String benefits;

    @Size(max = 200, message = "使用限制不能超过200个字符")
    private String usageLimit;

    @Size(max = 100, message = "有效期不能超过100个字符")
    private String validPeriod;

    @NotNull(message = "用户等级要求不能为空")
    @Min(value = 1, message = "用户等级要求必须大于等于1")
    private Integer userLevelRequired;

    @Min(value = 0, message = "库存不能小于0")
    private int stock;
}
