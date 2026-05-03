package com.concert.ticketing.show.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("shows")
public class Show {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String showName;

    private LocalDateTime showTime;

    private String venue;

    private String priceRange;

    private Integer totalSeats;

    private String coverImage;

    private String description;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
