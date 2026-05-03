package com.concert.ticketing.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long showId;

    private Long ticketId;

    private String seatNumber;

    private BigDecimal amount;

    private String ticketType;

    private String payStatus;

    private LocalDateTime orderTime;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String showName;
}
