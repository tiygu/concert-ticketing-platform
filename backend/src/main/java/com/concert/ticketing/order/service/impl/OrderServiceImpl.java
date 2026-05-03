package com.concert.ticketing.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String PAY_STATUS_PENDING = "PENDING";
    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_CANCELLED = "CANCELLED";
    private static final String PAY_STATUS_REJECTED = "REJECTED";
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String ACTION_REJECT = "REJECT";

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
        return getUserOrders(userId, null);
    }

    @Override
    public List<Order> getUserOrders(Long userId, String payStatus) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId);
        if (payStatus != null && !payStatus.isBlank()) {
            wrapper.eq(Order::getPayStatus, payStatus.trim());
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        List<Order> orders = orderMapper.selectList(wrapper);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(404, "订单不存在");
        }
        if (PAY_STATUS_PAID.equals(order.getPayStatus())) {
            enrichShowName(order);
            return order;
        }
        if (!PAY_STATUS_PENDING.equals(order.getPayStatus())) {
            throw new BizException(400, "只有待支付订单可以支付");
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = updatePayStatus(orderId, PAY_STATUS_PENDING, PAY_STATUS_PAID, now, true);
        if (affected == 0) {
            Order latestOrder = orderMapper.selectById(orderId);
            if (latestOrder != null && PAY_STATUS_PAID.equals(latestOrder.getPayStatus())) {
                enrichShowName(latestOrder);
                return latestOrder;
            }
            throw new BizException(409, "订单状态已变更，请刷新后重试");
        }

        Order paidOrder = orderMapper.selectById(orderId);
        enrichShowName(paidOrder);
        log.info("Order paid: orderNo={}, userId={}", paidOrder.getOrderNo(), userId);
        return paidOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(404, "订单不存在");
        }
        if (!PAY_STATUS_PENDING.equals(order.getPayStatus())) {
            throw new BizException(400, "只有待支付订单可以取消");
        }

        LocalDateTime now = LocalDateTime.now();
        ticketService.releaseSeat(order.getTicketId());
        int affected = updatePayStatus(orderId, PAY_STATUS_PENDING, PAY_STATUS_CANCELLED, now, false);
        if (affected == 0) {
            throw new BizException(409, "订单状态已变更，请刷新后重试");
        }
        log.info("Order cancelled: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public IPage<Order> getAllOrders(Integer page, Integer size, String payStatus) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;

        LambdaQueryWrapper<Order> countWrapper = new LambdaQueryWrapper<>();
        addPayStatusFilter(countWrapper, payStatus);
        long total = orderMapper.selectCount(countWrapper);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        addPayStatusFilter(wrapper, payStatus);
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        orderPage.setTotal(total);
        enrichShowNames(orderPage.getRecords());
        return orderPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order auditOrder(Long orderId, String action) {
        if (action == null || action.isBlank()) {
            throw new BizException(400, "审核动作不能为空");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!PAY_STATUS_PENDING.equals(order.getPayStatus())) {
            throw new BizException(400, "只有待支付订单可以审核");
        }

        String normalizedAction = action.trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if (ACTION_APPROVE.equals(normalizedAction)) {
            int affected = updatePayStatus(orderId, PAY_STATUS_PENDING, PAY_STATUS_PAID, now, true);
            if (affected == 0) {
                throw new BizException(409, "订单状态已变更，请刷新后重试");
            }
            Order approvedOrder = orderMapper.selectById(orderId);
            enrichShowName(approvedOrder);
            log.info("Order audit approved: orderNo={}", approvedOrder.getOrderNo());
            return approvedOrder;
        }
        if (ACTION_REJECT.equals(normalizedAction)) {
            ticketService.releaseSeat(order.getTicketId());
            int affected = updatePayStatus(orderId, PAY_STATUS_PENDING, PAY_STATUS_REJECTED, now, false);
            if (affected == 0) {
                throw new BizException(409, "订单状态已变更，请刷新后重试");
            }
            Order rejectedOrder = orderMapper.selectById(orderId);
            enrichShowName(rejectedOrder);
            log.info("Order audit rejected: orderNo={}", rejectedOrder.getOrderNo());
            return rejectedOrder;
        }

        throw new BizException(400, "不支持的审核动作");
    }

    private int updatePayStatus(Long orderId, String fromStatus, String toStatus, LocalDateTime now, boolean updatePayTime) {
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getPayStatus, fromStatus)
                .set(Order::getPayStatus, toStatus)
                .set(Order::getUpdatedAt, now);
        if (updatePayTime) {
            wrapper.set(Order::getPayTime, now);
        }
        return orderMapper.update(null, wrapper);
    }

    private void addPayStatusFilter(LambdaQueryWrapper<Order> wrapper, String payStatus) {
        if (payStatus != null && !payStatus.isBlank()) {
            wrapper.eq(Order::getPayStatus, payStatus.trim());
        }
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
