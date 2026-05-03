package com.concert.ticketing.statistics.service;

import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.statistics.dto.RevenueDetailItem;
import com.concert.ticketing.statistics.dto.RevenueReportResponse;
import com.concert.ticketing.statistics.dto.StockDetailItem;
import com.concert.ticketing.statistics.dto.StockStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(statements = "DELETE FROM orders; DELETE FROM tickets; DELETE FROM shows; DELETE FROM users", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("StatisticsService 集成测试")
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should_按演出汇总营收_when_存在已支付和已完成订单")
    void getRevenueReport_withPaidAndCompletedOrders_returnsRevenueByShow() {
        insertUser(100L, "admin100", "ADMIN");
        insertShow(200L, "星河巡演北京站");
        insertShow(201L, "星河巡演上海站");
        insertTicket(300L, 200L, "REGULAR", "看台", BigDecimal.valueOf(380), 10, 2);
        insertTicket(301L, 201L, "VIP", "VIP区", BigDecimal.valueOf(1280), 5, 1);
        insertOrder(400L, 100L, 200L, 300L, "PAID", BigDecimal.valueOf(380), LocalDateTime.of(2026, 5, 1, 10, 0));
        insertOrder(401L, 100L, 200L, 300L, "COMPLETED", BigDecimal.valueOf(580), LocalDateTime.of(2026, 5, 2, 11, 0));
        insertOrder(402L, 100L, 201L, 301L, "COMPLETED", BigDecimal.valueOf(1280), LocalDateTime.of(2026, 5, 3, 12, 0));

        RevenueReportResponse response = statisticsService.getRevenueReport(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

        assertThat(response.getTotalOrders()).isEqualTo(3L);
        assertThat(response.getTotalTickets()).isEqualTo(3L);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("2240.00");
        assertThat(response.getDetails()).hasSize(2);

        Map<Long, RevenueDetailItem> detailMap = response.getDetails().stream()
                .collect(Collectors.toMap(RevenueDetailItem::getShowId, Function.identity()));
        assertThat(detailMap.get(200L).getShowName()).isEqualTo("星河巡演北京站");
        assertThat(detailMap.get(200L).getOrderCount()).isEqualTo(2L);
        assertThat(detailMap.get(200L).getTicketCount()).isEqualTo(2L);
        assertThat(detailMap.get(200L).getRevenue()).isEqualByComparingTo("960.00");
        assertThat(detailMap.get(201L).getShowName()).isEqualTo("星河巡演上海站");
        assertThat(detailMap.get(201L).getOrderCount()).isEqualTo(1L);
        assertThat(detailMap.get(201L).getRevenue()).isEqualByComparingTo("1280.00");
    }

    @Test
    @DisplayName("should_返回零营收_when_日期为空")
    void getRevenueReport_emptyDateRange_returnsZeros() {
        insertPaidOrderFixture(400L, LocalDateTime.of(2026, 5, 1, 10, 0));

        RevenueReportResponse response = statisticsService.getRevenueReport(null, LocalDate.of(2026, 5, 31));

        assertZeroRevenue(response);
    }

    @Test
    @DisplayName("should_返回零营收_when_日期范围内无订单")
    void getRevenueReport_noOrdersInRange_returnsZeros() {
        insertPaidOrderFixture(400L, LocalDateTime.of(2026, 1, 15, 10, 0));

        RevenueReportResponse response = statisticsService.getRevenueReport(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        assertZeroRevenue(response);
    }

    @Test
    @DisplayName("should_排除未支付取消驳回订单_when_统计营收")
    void getRevenueReport_excludesPendingCancelledRejectedOrders() {
        insertUser(100L, "admin100", "ADMIN");
        insertShow(200L, "状态过滤演出");
        insertTicket(300L, 200L, "REGULAR", "内场", BigDecimal.valueOf(680), 10, 4);
        insertOrder(400L, 100L, 200L, 300L, "PENDING", BigDecimal.valueOf(680), LocalDateTime.of(2026, 5, 1, 10, 0));
        insertOrder(401L, 100L, 200L, 300L, "CANCELLED", BigDecimal.valueOf(680), LocalDateTime.of(2026, 5, 1, 11, 0));
        insertOrder(402L, 100L, 200L, 300L, "REJECTED", BigDecimal.valueOf(680), LocalDateTime.of(2026, 5, 1, 12, 0));
        insertOrder(403L, 100L, 200L, 300L, "COMPLETED", BigDecimal.valueOf(680), LocalDateTime.of(2026, 5, 1, 13, 0));

        RevenueReportResponse response = statisticsService.getRevenueReport(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1));

        assertThat(response.getTotalOrders()).isEqualTo(1L);
        assertThat(response.getTotalTickets()).isEqualTo(1L);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("680.00");
        assertThat(response.getDetails()).hasSize(1);
        assertThat(response.getDetails().get(0).getOrderCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should_返回库存统计_when_演出存在票品")
    void getStockStats_validShow_returnsCorrectStats() {
        insertShow(200L, "库存统计演出");
        insertTicket(300L, 200L, "REGULAR", "看台", BigDecimal.valueOf(380), 100, 50);
        insertTicket(301L, 200L, "VIP", "VIP区", BigDecimal.valueOf(1280), 80, 20);

        StockStatsResponse response = statisticsService.getStockStats(200L);

        assertThat(response.getShowId()).isEqualTo(200L);
        assertThat(response.getShowName()).isEqualTo("库存统计演出");
        assertThat(response.getTotalStock()).isEqualTo(180);
        assertThat(response.getTotalSold()).isEqualTo(70);
        assertThat(response.getTotalCapacity()).isEqualTo(250);
        assertThat(response.getSoldPercentage()).isEqualTo(28.0);
        assertThat(response.getDetails()).hasSize(2);

        Map<Long, StockDetailItem> detailMap = response.getDetails().stream()
                .collect(Collectors.toMap(StockDetailItem::getTicketId, Function.identity()));
        assertThat(detailMap.get(300L).getTicketType()).isEqualTo("REGULAR");
        assertThat(detailMap.get(300L).getPriceType()).isEqualTo("看台");
        assertThat(detailMap.get(300L).getPrice()).isEqualByComparingTo("380.00");
        assertThat(detailMap.get(300L).getStock()).isEqualTo(100);
        assertThat(detailMap.get(300L).getSold()).isEqualTo(50);
        assertThat(detailMap.get(300L).getTotal()).isEqualTo(150);
        assertThat(detailMap.get(300L).getSoldPercentage()).isEqualTo(33.3);
        assertThat(detailMap.get(301L).getSoldPercentage()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("should_抛出404_when_演出不存在")
    void getStockStats_showNotFound_throwsException() {
        assertThatThrownBy(() -> statisticsService.getStockStats(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("演出不存在")
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("should_返回零库存统计_when_演出无票品")
    void getStockStats_showWithNoTickets_returnsZeroStats() {
        insertShow(200L, "无票品演出");

        StockStatsResponse response = statisticsService.getStockStats(200L);

        assertThat(response.getShowId()).isEqualTo(200L);
        assertThat(response.getShowName()).isEqualTo("无票品演出");
        assertThat(response.getTotalStock()).isZero();
        assertThat(response.getTotalSold()).isZero();
        assertThat(response.getTotalCapacity()).isZero();
        assertThat(response.getSoldPercentage()).isEqualTo(0.0);
        assertThat(response.getDetails()).isEmpty();
    }

    @Test
    @DisplayName("should_聚合同一演出多笔订单_when_统计营收")
    void getRevenueReport_singleShowMultipleOrders_aggregates() {
        insertUser(100L, "admin100", "ADMIN");
        insertShow(200L, "单场多订单演出");
        insertTicket(300L, 200L, "REGULAR", "内场", BigDecimal.valueOf(680), 10, 2);
        insertOrder(400L, 100L, 200L, 300L, "PAID", BigDecimal.valueOf(680), LocalDateTime.of(2026, 5, 10, 10, 0));
        insertOrder(401L, 100L, 200L, 300L, "PAID", BigDecimal.valueOf(880), LocalDateTime.of(2026, 5, 10, 11, 0));

        RevenueReportResponse response = statisticsService.getRevenueReport(
                LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 10));

        assertThat(response.getTotalOrders()).isEqualTo(2L);
        assertThat(response.getTotalTickets()).isEqualTo(2L);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo("1560.00");
        assertThat(response.getDetails()).hasSize(1);
        RevenueDetailItem detail = response.getDetails().get(0);
        assertThat(detail.getShowId()).isEqualTo(200L);
        assertThat(detail.getShowName()).isEqualTo("单场多订单演出");
        assertThat(detail.getOrderCount()).isEqualTo(2L);
        assertThat(detail.getTicketCount()).isEqualTo(2L);
        assertThat(detail.getRevenue()).isEqualByComparingTo("1560.00");
    }

    private void assertZeroRevenue(RevenueReportResponse response) {
        assertThat(response.getTotalOrders()).isZero();
        assertThat(response.getTotalTickets()).isZero();
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDetails()).isEmpty();
    }

    private void insertPaidOrderFixture(Long orderId, LocalDateTime orderTime) {
        insertUser(100L, "admin100", "ADMIN");
        insertShow(200L, "测试演出");
        insertTicket(300L, 200L, "REGULAR", "看台", BigDecimal.valueOf(380), 10, 1);
        insertOrder(orderId, 100L, 200L, 300L, "PAID", BigDecimal.valueOf(380), orderTime);
    }

    private void insertUser(Long id, String username, String role) {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password, role, vip_level, points, status, is_deleted) " +
                        "VALUES (?, ?, ?, ?, 0, 0, 'ACTIVE', 0)",
                id, username, "password", role);
    }

    private void insertShow(Long id, String showName) {
        jdbcTemplate.update(
                "INSERT INTO shows (id, show_name, show_time, venue, price_range, total_seats, status, is_deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'UPCOMING', 0)",
                id, showName, LocalDateTime.of(2026, 6, 1, 20, 0), "测试场馆", "100-1280", 1000);
    }

    private void insertTicket(Long id, Long showId, String ticketType, String priceType,
                              BigDecimal price, Integer stock, Integer sold) {
        jdbcTemplate.update(
                "INSERT INTO tickets (id, show_id, ticket_type, price_type, seat_number, price, stock, sold) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, showId, ticketType, priceType, priceType + "-" + id, price, stock, sold);
    }

    private void insertOrder(Long id, Long userId, Long showId, Long ticketId, String payStatus,
                             BigDecimal amount, LocalDateTime orderTime) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, order_no, user_id, show_id, ticket_id, seat_number, amount, ticket_type, " +
                        "pay_status, order_time, pay_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'REGULAR', ?, ?, ?)",
                id, "ORD" + id, userId, showId, ticketId, "A-" + ticketId, amount, payStatus,
                orderTime, orderTime.plusMinutes(5));
    }
}
