package com.concert.ticketing.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.common.dto.PageResult;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.dto.AdminOrderResponse;
import com.concert.ticketing.order.dto.AuditOrderRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public Result<List<OrderResponse>> listOrders(@RequestParam(required = false) String status) {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        List<OrderResponse> orders = orderService.getUserOrders(user.getId(), status).stream()
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

    @PutMapping("/api/user/orders/{id}/pay")
    public Result<OrderResponse> payOrder(@PathVariable Long id) {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        Order order = orderService.payOrder(id, user.getId());
        return Result.ok(OrderResponse.from(order));
    }

    @PutMapping("/api/user/orders/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        String username = getCurrentUsername();
        User user = getCurrentUser(username);
        orderService.cancelOrder(id, user.getId());
        return Result.ok();
    }

    @GetMapping("/api/admin/orders")
    public Result<PageResult<AdminOrderResponse>> listAllOrders(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) String status) {
        IPage<Order> orderPage = orderService.getAllOrders(page, size, status);
        Map<Long, String> usernameMap = getUsernameMap(orderPage.getRecords());
        List<AdminOrderResponse> records = orderPage.getRecords().stream()
                .map(order -> AdminOrderResponse.from(order, usernameMap.get(order.getUserId())))
                .collect(Collectors.toList());
        return Result.ok(new PageResult<>(records, orderPage.getTotal(), page, size));
    }

    @PutMapping("/api/admin/orders/{id}/audit")
    public Result<OrderResponse> auditOrder(@PathVariable Long id, @Valid @RequestBody AuditOrderRequest request) {
        Order order = orderService.auditOrder(id, request.getAction());
        return Result.ok(OrderResponse.from(order));
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

    private Map<Long, String> getUsernameMap(List<Order> orders) {
        Set<Long> userIds = orders.stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }
}
