package com.concert.ticketing.review.service;

import com.concert.ticketing.review.dto.ReviewCreateRequest;
import com.concert.ticketing.review.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(Long userId, ReviewCreateRequest request);

    List<ReviewResponse> listShowReviews(Long showId);
}
