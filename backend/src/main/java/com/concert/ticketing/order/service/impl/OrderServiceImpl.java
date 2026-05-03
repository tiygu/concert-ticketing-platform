package com.concert.ticketing.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.dto.OrderCreateRequest;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.mapper.OrderMapper;
import com.concert.ticketing.show.entity.Show;
import com.concert.ticketing.show.mapper.ShowMapper;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String PAY_STATUS_PENDING = "PENDING";

    private final OrderMapper orderMapper;
    private final TicketService ticketService;
    private final ShowMapper showMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Long userId, String username, OrderCreateRequest request) {
        Show show = showMapper.selectById(request.getShowId());
        if (show == null) {
            throw new BizException(404, "演出不存在");
        }

        Ticket ticket = ticketService.getById(request.getTicketId());
        if (ticket == null) {
            throw new BizException(404, "选票不存在");
        }
        if (!ticket.getShowId().equals(request.getShowId())) {
            throw new BizException(400, "选票不属于该演出");
        }

        Ticket lockedTicket = ticketService.lockSeat(request.getTicketId());
        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();
        order.setOrderNo("ORD" + System.currentTimeMillis());
        order.setUserId(userId);
        order.setShowId(request.getShowId());
        order.setTicketId(request.getTicketId());
        order.setSeatNumber(lockedTicket.getSeatNumber());
        order.setAmount(lockedTicket.getPrice());
        order.setTicketType(lockedTicket.getTicketType());
        order.setPayStatus(PAY_STATUS_PENDING);
        order.setOrderTime(now);
        order.setExpireTime(now.plusMinutes(15));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setShowName(show.getShowName());
        orderMapper.insert(order);

        log.info("Order created: orderNo={}, userId={}, username={}, ticketId={}",
                order.getOrderNo(), userId, username, request.getTicketId());
        return order;
    }

    @Override
    public List<Order> getUserOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
        enrichShowNames(orders);
        return orders;
    }

    @Override
    public Order getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(404, "订单不存在");
        }
        enrichShowName(order);
        return order;
    }

    private void enrichShowNames(List<Order> orders) {
        for (Order order : orders) {
            enrichShowName(order);
        }
    }

    private void enrichShowName(Order order) {
        Show show = showMapper.selectById(order.getShowId());
        if (show != null) {
            order.setShowName(show.getShowName());
        }
    }
}
