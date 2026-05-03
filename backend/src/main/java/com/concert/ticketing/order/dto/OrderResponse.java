package com.concert.ticketing.order.dto;

import com.concert.ticketing.order.entity.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long showId;
    private Long ticketId;
    private String seatNumber;
    private BigDecimal amount;
    private String ticketType;
    private String payStatus;
    private String statusText;
    private LocalDateTime orderTime;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private String showName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setShowId(order.getShowId());
        response.setTicketId(order.getTicketId());
        response.setSeatNumber(order.getSeatNumber());
        response.setAmount(order.getAmount());
        response.setTicketType(order.getTicketType());
        response.setPayStatus(order.getPayStatus());
        response.setStatusText(toStatusText(order.getPayStatus()));
        response.setOrderTime(order.getOrderTime());
        response.setPayTime(order.getPayTime());
        response.setExpireTime(order.getExpireTime());
        response.setShowName(order.getShowName());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    private static String toStatusText(String payStatus) {
        if (payStatus == null) {
            return null;
        }
        switch (payStatus) {
            case "PENDING":
                return "待支付";
            case "PAID":
                return "已支付";
            case "COMPLETED":
                return "已完成";
            case "CANCELLED":
                return "已取消";
            case "REJECTED":
                return "已驳回";
            default:
                return payStatus;
        }
    }
}
