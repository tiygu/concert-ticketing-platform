package com.concert.ticketing.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reviews")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long showId;

    private String content;

    private Integer rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
