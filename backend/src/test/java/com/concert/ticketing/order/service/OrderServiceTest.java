package com.concert.ticketing.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.order.dto.OrderCreateRequest;
import com.concert.ticketing.order.entity.Order;
import com.concert.ticketing.order.mapper.OrderMapper;
import com.concert.ticketing.ticket.entity.Ticket;
import com.concert.ticketing.ticket.service.TicketService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("OrderService 集成测试")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM tickets");
        jdbcTemplate.execute("DELETE FROM vip_bookings");
        jdbcTemplate.execute("DELETE FROM vip_packages");
        jdbcTemplate.execute("DELETE FROM notices");
        jdbcTemplate.execute("DELETE FROM shows");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("should_返回座位可用状态_when_查询演出座位列表")
    void should_return_seats_with_available_status_when_listing_show_seats() throws Exception {
        insertShow(1L, "座位测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 1, 0);
        insertTicket(2L, 1L, "VIP", "VIP区", "V1", 680.00, 0, 1);

        mockMvc.perform(get("/api/shows/1/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$.data[0].available").value(true))
                .andExpect(jsonPath("$.data[1].seatNumber").value("V1"))
                .andExpect(jsonPath("$.data[1].available").value(false));
    }

    @Test
    @DisplayName("should_创建订单并扣减库存_when_选票有效")
    void should_create_order_and_deduct_stock_when_ticket_valid() {
        insertUser(100L, "alice");
        insertShow(1L, "订单测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 2, 0);

        Order order = orderService.createOrder(100L, "alice", createRequest(1L, 1L));

        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNo()).startsWith("ORD");
        assertThat(order.getUserId()).isEqualTo(100L);
        assertThat(order.getSeatNumber()).isEqualTo("A1");
        assertThat(order.getPayStatus()).isEqualTo("PENDING");
        assertThat(order.getShowName()).isEqualTo("订单测试演唱会");

        Ticket ticket = ticketService.getById(1L);
        assertThat(ticket.getStock()).isEqualTo(1);
        assertThat(ticket.getSold()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_抛出异常_when_选票库存不足")
    void should_throw_exception_when_ticket_out_of_stock() {
        insertUser(100L, "alice");
        insertShow(1L, "售罄演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);

        assertThatThrownBy(() -> orderService.createOrder(100L, "alice", createRequest(1L, 1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("库存不足");

        assertThat(orderMapper.selectList(null)).isEmpty();
        Ticket ticket = ticketService.getById(1L);
        assertThat(ticket.getStock()).isZero();
        assertThat(ticket.getSold()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_抛出异常_when_演出不存在")
    void should_throw_exception_when_show_not_found() {
        insertUser(100L, "alice");

        assertThatThrownBy(() -> orderService.createOrder(100L, "alice", createRequest(999L, 1L)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("演出不存在");
    }

    @Test
    @DisplayName("should_创建待支付订单并设置15分钟过期时间")
    void should_create_pending_order_with_expire_time_15_minutes_later() {
        insertUser(100L, "alice");
        insertShow(1L, "过期时间测试演唱会");
        insertTicket(1L, 1L, "VIP", "VIP区", "V1", 680.00, 1, 0);

        LocalDateTime before = LocalDateTime.now();
        Order order = orderService.createOrder(100L, "alice", createRequest(1L, 1L));
        LocalDateTime after = LocalDateTime.now();

        assertThat(order.getPayStatus()).isEqualTo("PENDING");
        assertThat(order.getOrderTime()).isBetween(before, after);
        assertThat(order.getExpireTime()).isBetween(before.plusMinutes(15), after.plusMinutes(15));
    }

    @Test
    @DisplayName("should_只返回当前用户订单_when_查询用户订单列表")
    void should_return_only_current_user_orders_when_listing_user_orders() {
        insertUser(100L, "alice");
        insertUser(101L, "bob");
        insertShow(1L, "用户订单演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 1, 0);
        insertTicket(2L, 1L, "REGULAR", "看台", "A2", 280.00, 1, 0);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");
        insertOrder(2L, "ORD2", 101L, 1L, 2L, "A2", "2026-05-03 11:00:00");

        List<Order> orders = orderService.getUserOrders(100L);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUserId()).isEqualTo(100L);
        assertThat(orders.get(0).getShowName()).isEqualTo("用户订单演唱会");
    }

    @Test
    @DisplayName("should_校验订单归属_when_查询订单详情")
    void should_verify_ownership_when_getting_order_detail() {
        insertUser(100L, "alice");
        insertUser(101L, "bob");
        insertShow(1L, "归属测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 1, 0);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");

        Order order = orderService.getOrderDetail(1L, 100L);
        assertThat(order.getSeatNumber()).isEqualTo("A1");

        assertThatThrownBy(() -> orderService.getOrderDetail(1L, 101L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("订单不存在");
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("should_通过接口创建订单并返回状态文本")
    void should_create_order_and_return_status_text_when_calling_user_order_api() throws Exception {
        insertUser(100L, "alice");
        insertShow(1L, "接口订单演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 1, 0);

        mockMvc.perform(post("/api/user/orders")
                        .contentType("application/json")
                        .content("{\"showId\":1,\"ticketId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.payStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.statusText").value("待支付"))
                .andExpect(jsonPath("$.data.showName").value("接口订单演唱会"));
    }

    @Test
    @DisplayName("should_支付订单并支持重复支付幂等返回")
    void should_pay_order_and_return_paid_order_when_already_paid() {
        insertUser(100L, "alice");
        insertShow(1L, "支付测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");

        Order paidOrder = orderService.payOrder(1L, 100L);
        Order paidAgain = orderService.payOrder(1L, 100L);

        assertThat(paidOrder.getPayStatus()).isEqualTo("PAID");
        assertThat(paidOrder.getPayTime()).isNotNull();
        assertThat(paidOrder.getShowName()).isEqualTo("支付测试演唱会");
        assertThat(paidAgain.getPayStatus()).isEqualTo("PAID");
        assertThat(paidAgain.getPayTime()).isEqualTo(paidOrder.getPayTime());
    }

    @Test
    @DisplayName("should_取消待支付订单并释放座位")
    void should_cancel_pending_order_and_release_seat() {
        insertUser(100L, "alice");
        insertShow(1L, "取消测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");

        orderService.cancelOrder(1L, 100L);

        Order order = orderMapper.selectById(1L);
        Ticket ticket = ticketService.getById(1L);
        assertThat(order.getPayStatus()).isEqualTo("CANCELLED");
        assertThat(ticket.getStock()).isEqualTo(1);
        assertThat(ticket.getSold()).isZero();
    }

    @Test
    @DisplayName("should_按支付状态筛选用户订单")
    void should_filter_user_orders_by_pay_status() {
        insertUser(100L, "alice");
        insertShow(1L, "筛选测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertTicket(2L, 1L, "REGULAR", "看台", "A2", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");
        insertOrder(2L, "ORD2", 100L, 1L, 2L, "A2", "2026-05-03 11:00:00");
        jdbcTemplate.update("UPDATE orders SET pay_status = 'PAID' WHERE id = 2");

        List<Order> orders = orderService.getUserOrders(100L, "PAID");

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderNo()).isEqualTo("ORD2");
        assertThat(orders.get(0).getShowName()).isEqualTo("筛选测试演唱会");
    }

    @Test
    @DisplayName("should_审核通过或驳回待支付订单")
    void should_audit_pending_orders_with_approve_or_reject() {
        insertUser(100L, "alice");
        insertShow(1L, "审核测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertTicket(2L, 1L, "REGULAR", "看台", "A2", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");
        insertOrder(2L, "ORD2", 100L, 1L, 2L, "A2", "2026-05-03 11:00:00");

        Order approvedOrder = orderService.auditOrder(1L, "APPROVE");
        Order rejectedOrder = orderService.auditOrder(2L, "REJECT");

        Ticket rejectedTicket = ticketService.getById(2L);
        assertThat(approvedOrder.getPayStatus()).isEqualTo("PAID");
        assertThat(approvedOrder.getPayTime()).isNotNull();
        assertThat(rejectedOrder.getPayStatus()).isEqualTo("REJECTED");
        assertThat(rejectedTicket.getStock()).isEqualTo(1);
        assertThat(rejectedTicket.getSold()).isZero();
    }

    @Test
    @DisplayName("should_分页查询全部订单并按状态筛选")
    void should_page_all_orders_and_filter_by_pay_status() {
        insertUser(100L, "alice");
        insertUser(101L, "bob");
        insertShow(1L, "后台列表演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertTicket(2L, 1L, "REGULAR", "看台", "A2", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");
        insertOrder(2L, "ORD2", 101L, 1L, 2L, "A2", "2026-05-03 11:00:00");
        jdbcTemplate.update("UPDATE orders SET pay_status = 'PAID' WHERE id = 2");

        IPage<Order> orderPage = orderService.getAllOrders(1, 10, "PAID");

        assertThat(orderPage.getTotal()).isEqualTo(1);
        assertThat(orderPage.getRecords()).hasSize(1);
        assertThat(orderPage.getRecords().get(0).getOrderNo()).isEqualTo("ORD2");
        assertThat(orderPage.getRecords().get(0).getShowName()).isEqualTo("后台列表演唱会");
    }

    @Test
    @DisplayName("should_超时取消待支付订单并释放座位")
    void should_cancel_expired_pending_orders_and_release_seats() {
        insertUser(100L, "alice");
        insertShow(1L, "超时测试演唱会");
        insertTicket(1L, 1L, "REGULAR", "看台", "A1", 280.00, 0, 1);
        insertOrder(1L, "ORD1", 100L, 1L, 1L, "A1", "2026-05-03 10:00:00");
        jdbcTemplate.update("UPDATE orders SET expire_time = ? WHERE id = ?", LocalDateTime.now().minusMinutes(1), 1L);

        orderTimeoutService.cancelExpiredOrders();

        Order order = orderMapper.selectById(1L);
        Ticket ticket = ticketService.getById(1L);
        assertThat(order.getPayStatus()).isEqualTo("CANCELLED");
        assertThat(ticket.getStock()).isEqualTo(1);
        assertThat(ticket.getSold()).isZero();
    }

    private OrderCreateRequest createRequest(Long showId, Long ticketId) {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setShowId(showId);
        request.setTicketId(ticketId);
        return request;
    }

    private void insertUser(Long id, String username) {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password, role, vip_level, points, status, is_deleted) " +
                        "VALUES (?, ?, ?, 'USER', 0, 0, 'ACTIVE', 0)",
                id, username, "password");
    }

    private void insertShow(Long id, String showName) {
        jdbcTemplate.update(
                "INSERT INTO shows (id, show_name, show_time, venue, price_range, total_seats, status, is_deleted) " +
                        "VALUES (?, ?, '2026-06-01 19:00:00', '场馆A', '280', 100, 'ON_SALE', 0)",
                id, showName);
    }

    private void insertTicket(Long id, Long showId, String ticketType, String priceType,
                              String seatNumber, double price, int stock, int sold) {
        jdbcTemplate.update(
                "INSERT INTO tickets (id, show_id, ticket_type, price_type, seat_number, price, stock, sold) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, showId, ticketType, priceType, seatNumber, BigDecimal.valueOf(price), stock, sold);
    }

    private void insertOrder(Long id, String orderNo, Long userId, Long showId, Long ticketId,
                             String seatNumber, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, order_no, user_id, show_id, ticket_id, seat_number, amount, ticket_type, " +
                        "pay_status, order_time, expire_time, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 280.00, 'REGULAR', 'PENDING', ?, ?, ?, ?)",
                id, orderNo, userId, showId, ticketId, seatNumber,
                createdAt, "2026-05-03 10:15:00", createdAt, createdAt);
    }
}
