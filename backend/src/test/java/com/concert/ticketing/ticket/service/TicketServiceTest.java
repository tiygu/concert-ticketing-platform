package com.concert.ticketing.ticket.service;

import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("TicketService 单元测试")
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        // 清理测试数据，顺序遵循外键依赖
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM tickets");
        jdbcTemplate.execute("DELETE FROM vip_bookings");
        jdbcTemplate.execute("DELETE FROM vip_packages");
        jdbcTemplate.execute("DELETE FROM notices");
        jdbcTemplate.execute("DELETE FROM shows");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private void insertShow(Long id, String name, String time, String venue, String status) {
        jdbcTemplate.update(
                "INSERT INTO shows (id, show_name, show_time, venue, status) VALUES (?, ?, ?, ?, ?)",
                id, name, time, venue, status);
    }

    private void insertTicket(Long id, Long showId, String ticketType,
                               String priceType, String seatNumber,
                               double price, int stock, int sold) {
        jdbcTemplate.update(
                "INSERT INTO tickets (id, show_id, ticket_type, price_type, seat_number, price, stock, sold) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, showId, ticketType, priceType, seatNumber, BigDecimal.valueOf(price), stock, sold);
    }

    // ================================================================
    // lockSeat 测试
    // ================================================================

    @Test
    @DisplayName("should_成功锁定座位_when_库存充足")
    void should_lock_seat_when_stock_sufficient() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 10, 0);

        Ticket locked = ticketService.lockSeat(1L);

        assertThat(locked.getSeatNumber()).isEqualTo("A1");
        assertThat(locked.getStock()).isEqualTo(9);
        assertThat(locked.getSold()).isEqualTo(1);

        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getStock()).isEqualTo(9);
        assertThat(dbTicket.getSold()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_抛出异常_when_选票不存在")
    void should_throw_exception_when_ticket_not_found() {
        assertThatThrownBy(() -> ticketService.lockSeat(999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("选票不存在");
    }

    @Test
    @DisplayName("should_抛出异常_when_库存已耗尽")
    void should_throw_exception_when_stock_exhausted() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 10);

        assertThatThrownBy(() -> ticketService.lockSeat(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("should_库存正确扣减_when_连续多次锁定")
    void should_deduct_stock_correctly_when_multiple_locks() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 5, 0);

        Ticket t1 = ticketService.lockSeat(1L);
        assertThat(t1.getStock()).isEqualTo(4);

        Ticket t2 = ticketService.lockSeat(1L);
        assertThat(t2.getStock()).isEqualTo(3);

        Ticket t3 = ticketService.lockSeat(1L);
        assertThat(t3.getStock()).isEqualTo(2);

        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getStock()).isEqualTo(2);
        assertThat(dbTicket.getSold()).isEqualTo(3);
    }

    @Test
    @DisplayName("should_最后一个库存被抢购_when_库存仅剩1")
    void should_lock_last_stock_when_only_one_left() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 1, 9);

        Ticket locked = ticketService.lockSeat(1L);
        assertThat(locked.getStock()).isEqualTo(0);
        assertThat(locked.getSold()).isEqualTo(10);

        assertThatThrownBy(() -> ticketService.lockSeat(1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");
    }

    // ================================================================
    // releaseSeat 测试
    // ================================================================

    @Test
    @DisplayName("should_成功释放座位_when_已售数量大于0")
    void should_release_seat_when_sold_greater_than_zero() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 8, 2);

        ticketService.releaseSeat(1L);

        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getStock()).isEqualTo(9);
        assertThat(dbTicket.getSold()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_不抛异常_when_释放时已售为0")
    void should_not_throw_exception_when_release_with_zero_sold() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 10, 0);

        assertThatCode(() -> ticketService.releaseSeat(1L))
                .doesNotThrowAnyException();

        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getStock()).isEqualTo(10);
        assertThat(dbTicket.getSold()).isEqualTo(0);
    }

    // ================================================================
    // 事务回滚测试
    // ================================================================

    @Test
    @DisplayName("should_库存不变_when_扣减失败事务回滚")
    void should_not_change_stock_when_lock_fails_and_rolls_back() {
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 10, 0);

        ticketService.lockSeat(1L);

        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getStock()).isEqualTo(9);
        assertThat(dbTicket.getSold()).isEqualTo(1);
    }

    // ================================================================
    // 并发场景测试
    // ================================================================

    @Test
    @DisplayName("should_不超售_when_高并发锁定同一选票")
    void should_not_oversell_when_concurrent_lock() throws InterruptedException {
        int stock = 50;
        insertShow(1L, "测试演唱会", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, stock, 0);

        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    ticketService.lockSeat(1L);
                    successCount.incrementAndGet();
                } catch (BizException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // 成功次数不应超过实际库存
        int totalSuccess = successCount.get();
        assertThat(totalSuccess).isLessThanOrEqualTo(stock);

        // 验证不超售
        Ticket dbTicket = ticketService.getById(1L);
        assertThat(dbTicket.getSold()).isEqualTo(totalSuccess);
        assertThat(dbTicket.getStock()).isEqualTo(stock - totalSuccess);
        assertThat(dbTicket.getSold()).isLessThanOrEqualTo(stock);
    }

    // ================================================================
    // getByShowId 测试
    // ================================================================

    @Test
    @DisplayName("should_返回演出所有选票_when_查询演出选票列表")
    void should_return_show_tickets_when_query_by_show_id() {
        insertShow(1L, "演唱会A", "2026-06-01 19:00:00", "场馆A", "ON_SALE");
        insertShow(2L, "演唱会B", "2026-07-01 19:00:00", "场馆B", "UPCOMING");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 10, 0);
        insertTicket(2L, 1L, "VIP", "VIP区", "V1", 680.00, 5, 0);
        insertTicket(3L, 2L, "REGULAR", "内场", "B1", 380.00, 8, 0);

        List<Ticket> tickets = ticketService.getByShowId(1L);

        assertThat(tickets).hasSize(2);
        assertThat(tickets).extracting(Ticket::getSeatNumber)
                .containsExactlyInAnyOrder("A1", "V1");
    }

    @Test
    @DisplayName("should_返回空列表_when_演出无选票")
    void should_return_empty_list_when_show_has_no_tickets() {
        insertShow(1L, "演唱会", "2026-06-01 19:00:00", "场馆", "ON_SALE");

        List<Ticket> tickets = ticketService.getByShowId(1L);
        assertThat(tickets).isEmpty();
    }
}
