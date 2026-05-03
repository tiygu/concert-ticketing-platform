package com.concert.ticketing.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.mapper.OrderMapper;
import com.concert.ticketing.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutService {

    private final OrderMapper orderMapper;
    private final TicketService ticketService;

    @Scheduled(fixedRate = 30000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getPayStatus, "PENDING")
                        .lt(Order::getExpireTime, now));
        for (Order order : expiredOrders) {
            ticketService.releaseSeat(order.getTicketId());
            order.setPayStatus("CANCELLED");
            order.setUpdatedAt(now);
            orderMapper.updateById(order);
            log.info("Timeout cancelled: orderNo={}", order.getOrderNo());
        }
        if (!expiredOrders.isEmpty()) {
            log.info("Timeout scan: {} orders cancelled", expiredOrders.size());
        }
    }
}
