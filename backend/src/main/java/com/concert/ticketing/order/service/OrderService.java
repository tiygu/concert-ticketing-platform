package com.concert.ticketing.order.service;

import com.concert.ticketing.order.dto.OrderCreateRequest;
import com.concert.ticketing.order.entity.Order;

import java.util.List;

public interface OrderService {

    Order createOrder(Long userId, String username, OrderCreateRequest request);

    List<Order> getUserOrders(Long userId);

    Order getOrderDetail(Long orderId, Long userId);
}
