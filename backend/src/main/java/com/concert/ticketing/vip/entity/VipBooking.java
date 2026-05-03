package com.concert.ticketing.vip.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vip_bookings")
public class VipBooking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packageId;

    private Long userId;

    private LocalDateTime bookingTime;

    private LocalDate useDate;

    private String auditStatus;

    private String adminReply;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
