package com.concert.ticketing.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.mapper.OrderMapper;
import com.concert.ticketing.show.entity.Show;
import com.concert.ticketing.show.mapper.ShowMapper;
import com.concert.ticketing.statistics.dto.RevenueDetailItem;
import com.concert.ticketing.statistics.dto.RevenueReportResponse;
import com.concert.ticketing.statistics.dto.StockDetailItem;
import com.concert.ticketing.statistics.dto.StockStatsResponse;
import com.concert.ticketing.statistics.service.StatisticsService;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_COMPLETED = "COMPLETED";

    private final OrderMapper orderMapper;
    private final ShowMapper showMapper;
    private final TicketMapper ticketMapper;

    @Override
    public RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return zeroRevenueResponse();
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getPayStatus, PAY_STATUS_PAID, PAY_STATUS_COMPLETED)
                .ge(Order::getOrderTime, start)
                .lt(Order::getOrderTime, end));
        if (orders.isEmpty()) {
            return zeroRevenueResponse();
        }

        Map<Long, List<Order>> ordersByShowId = orders.stream()
                .collect(Collectors.groupingBy(Order::getShowId));
        Map<Long, Show> showMap = getShowMap(ordersByShowId.keySet());

        List<RevenueDetailItem> details = ordersByShowId.entrySet().stream()
                .map(entry -> toRevenueDetail(entry.getKey(), entry.getValue(), showMap.get(entry.getKey())))
                .collect(Collectors.toList());

        Long totalOrders = details.stream()
                .mapToLong(RevenueDetailItem::getOrderCount)
                .sum();
        BigDecimal totalRevenue = details.stream()
                .map(RevenueDetailItem::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueReportResponse(totalOrders, totalOrders, totalRevenue, details);
    }

    @Override
    public StockStatsResponse getStockStats(Long showId) {
        Show show = showMapper.selectById(showId);
        if (show == null) {
            throw new BizException(404, "演出不存在");
        }

        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getShowId, showId));
        if (tickets.isEmpty()) {
            return new StockStatsResponse(show.getId(), show.getShowName(), 0, 0, 0, 0.0, Collections.emptyList());
        }

        Integer totalStock = tickets.stream()
                .mapToInt(ticket -> valueOrZero(ticket.getStock()))
                .sum();
        Integer totalSold = tickets.stream()
                .mapToInt(ticket -> valueOrZero(ticket.getSold()))
                .sum();
        Integer totalCapacity = totalStock + totalSold;
        List<StockDetailItem> details = tickets.stream()
                .map(this::toStockDetail)
                .collect(Collectors.toList());

        return new StockStatsResponse(show.getId(), show.getShowName(), totalStock, totalSold,
                totalCapacity, calculatePercentage(totalSold, totalCapacity), details);
    }

    private RevenueReportResponse zeroRevenueResponse() {
        return new RevenueReportResponse(0L, 0L, BigDecimal.ZERO, Collections.emptyList());
    }

    private Map<Long, Show> getShowMap(Set<Long> showIds) {
        if (showIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return showMapper.selectBatchIds(showIds).stream()
                .collect(Collectors.toMap(Show::getId, Function.identity()));
    }

    private RevenueDetailItem toRevenueDetail(Long showId, List<Order> orders, Show show) {
        long orderCount = orders.size();
        BigDecimal revenue = orders.stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueDetailItem(showId, show == null ? null : show.getShowName(), orderCount, orderCount, revenue);
    }

    private StockDetailItem toStockDetail(Ticket ticket) {
        Integer stock = valueOrZero(ticket.getStock());
        Integer sold = valueOrZero(ticket.getSold());
        Integer total = stock + sold;
        return new StockDetailItem(ticket.getId(), ticket.getTicketType(), ticket.getPriceType(), ticket.getPrice(),
                stock, sold, total, calculatePercentage(sold, total));
    }

    private Integer valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private Double calculatePercentage(Integer sold, Integer total) {
        if (total == null || total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(sold)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
