package com.concert.ticketing.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.dto.OrderCreateRequest;
import com.concert.ticketing.order.dto.OrderResponse;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.service.OrderService;
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
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    @PostMapping("/api/user/orders")
    public Result<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        Order order = orderService.createOrder(user.getId(), username, request);
        return Result.ok(OrderResponse.from(order));
    }

    @GetMapping("/api/user/orders")
    public Result<List<OrderResponse>> listOrders() {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        List<OrderResponse> orders = orderService.getUserOrders(user.getId()).stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
        return Result.ok(orders);
    }

    @GetMapping("/api/user/orders/{id}")
    public Result<OrderResponse> getOrder(@PathVariable Long id) {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        return Result.ok(OrderResponse.from(orderService.getOrderDetail(id, user.getId())));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser(String username) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        return user;
    }
}
