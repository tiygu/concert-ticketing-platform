package com.concert.ticketing.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.mapper.OrderMapper;
import com.concert.ticketing.review.dto.ReviewCreateRequest;
import com.concert.ticketing.review.dto.ReviewResponse;
import com.concert.ticketing.review.entity.Review;
import com.concert.ticketing.review.mapper.ReviewMapper;
import com.concert.ticketing.review.service.ReviewService;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponse createReview(Long userId, ReviewCreateRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(404, "订单不存在");
        }
        if (!ORDER_STATUS_COMPLETED.equals(order.getPayStatus())) {
            throw new BizException(400, "只有已完成订单可以评价");
        }
        if (!Objects.equals(order.getShowId(), request.getShowId())) {
            throw new BizException(400, "订单与演出不匹配");
        }

        Review existingReview = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId()));
        if (existingReview != null) {
            throw new BizException(400, "该订单已评价");
        }

        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setUserId(userId);
        review.setShowId(request.getShowId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        Review savedReview = reviewMapper.selectOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, request.getOrderId()));
        User user = userMapper.selectById(userId);

        log.info("Review created: reviewId={}, orderId={}, userId={}, showId={}",
                savedReview.getId(), request.getOrderId(), userId, request.getShowId());
        return toResponse(savedReview, user);
    }

    @Override
    public List<ReviewResponse> listShowReviews(Long showId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getShowId, showId)
                .orderByDesc(Review::getCreatedAt));
        Map<Long, User> userMap = getUserMap(reviews);
        return reviews.stream()
                .map(review -> toResponse(review, userMap.get(review.getUserId())))
                .collect(Collectors.toList());
    }

    private Map<Long, User> getUserMap(List<Review> reviews) {
        Set<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private ReviewResponse toResponse(Review review, User user) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setUserId(review.getUserId());
        response.setShowId(review.getShowId());
        response.setUsername(user == null ? null : user.getUsername());
        response.setContent(review.getContent());
        response.setRating(review.getRating());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
