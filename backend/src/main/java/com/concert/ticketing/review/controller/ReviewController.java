package com.concert.ticketing.review.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.review.dto.ReviewCreateRequest;
import com.concert.ticketing.review.dto.ReviewResponse;
import com.concert.ticketing.review.service.ReviewService;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserMapper userMapper;

    @PostMapping("/api/user/reviews")
    public Result<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new BizException(401, "用户未登录");
        }
        ReviewResponse response = reviewService.createReview(user.getId(), request);
        return Result.ok(response);
    }

    @GetMapping("/api/shows/{id}/reviews")
    public Result<List<ReviewResponse>> listShowReviews(@PathVariable Long id) {
        List<ReviewResponse> reviews = reviewService.listShowReviews(id);
        return Result.ok(reviews);
    }
}
