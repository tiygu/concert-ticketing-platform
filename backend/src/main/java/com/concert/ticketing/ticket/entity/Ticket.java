package com.concert.ticketing.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tickets")
public class Ticket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long showId;

    private String ticketType;

    private String priceType;

    private String seatNumber;

    private BigDecimal price;

    private Integer pointsPrice;

    private Integer stock;

    private Integer sold;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
