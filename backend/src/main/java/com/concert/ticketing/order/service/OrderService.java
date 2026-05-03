package com.concert.ticketing.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.order.dto.OrderCreateRequest;
import com.concert.ticketing.order.entity.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(Long userId, String username, OrderCreateRequest request);

    List<Order> getUserOrders(Long userId);

    List<Order> getUserOrders(Long userId, String payStatus);

    Order getOrderDetail(Long orderId, Long userId);

    Order payOrder(Long orderId, Long userId);

    void cancelOrder(Long orderId, Long userId);

    IPage<Order> getAllOrders(Integer page, Integer size, String payStatus);

    Order auditOrder(Long orderId, String action);
}
