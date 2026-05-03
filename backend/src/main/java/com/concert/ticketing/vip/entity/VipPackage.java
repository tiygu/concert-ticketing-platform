package com.concert.ticketing.vip.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vip_packages")
public class VipPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String packageName;

    private String benefits;

    private String usageLimit;

    private String validPeriod;

    private Integer userLevelRequired;

    private Integer stock;

    private Integer bookedCount;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
