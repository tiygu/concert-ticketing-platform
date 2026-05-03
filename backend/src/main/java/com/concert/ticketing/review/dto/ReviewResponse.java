package com.concert.ticketing.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private Long showId;
    private String username;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
}
