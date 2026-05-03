package com.concert.ticketing.review.service;

import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.review.dto.ReviewCreateRequest;
import com.concert.ticketing.review.dto.ReviewResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("ReviewService 集成测试")
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM tickets");
        jdbcTemplate.execute("DELETE FROM shows");
        jdbcTemplate.execute("DELETE FROM users WHERE username <> 'admin'");
    }

    @Test
    @DisplayName("should_保存评价并返回用户名_when_订单已完成")
    void createReview_completedOrder_success() {
        insertOrderFixture(1L, 100L, 200L, 300L, "COMPLETED");

        ReviewResponse response = reviewService.createReview(100L, createRequest(1L, 200L, 5, "演唱会体验很好"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getShowId()).isEqualTo(200L);
        assertThat(response.getUsername()).isEqualTo("user100");
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("演唱会体验很好");
        assertThat(response.getCreatedAt()).isNotNull();

        Integer reviewCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE order_id = ? AND user_id = ? AND show_id = ?",
                Integer.class, 1L, 100L, 200L);
        assertThat(reviewCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should_抛出400_when_订单未完成")
    void createReview_nonCompletedOrder_throwsException() {
        insertOrderFixture(1L, 100L, 200L, 300L, "PAID");

        assertThatThrownBy(() -> reviewService.createReview(100L, createRequest(1L, 200L, 4, "还不错")))
                .isInstanceOf(BizException.class)
                .hasMessage("只有已完成订单可以评价")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_抛出400_when_订单已评价")
    void createReview_duplicateReview_throwsException() {
        insertOrderFixture(1L, 100L, 200L, 300L, "COMPLETED");
        reviewService.createReview(100L, createRequest(1L, 200L, 5, "第一次评价"));

        assertThatThrownBy(() -> reviewService.createReview(100L, createRequest(1L, 200L, 4, "第二次评价")))
                .isInstanceOf(BizException.class)
                .hasMessage("该订单已评价")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_抛出404_when_评价他人订单")
    void createReview_wrongUser_throwsException() {
        insertOrderFixture(1L, 101L, 200L, 300L, "COMPLETED");
        insertUser(100L, "user100");

        assertThatThrownBy(() -> reviewService.createReview(100L, createRequest(1L, 200L, 5, "越权评价")))
                .isInstanceOf(BizException.class)
                .hasMessage("订单不存在")
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("should_抛出400_when_订单演出不匹配")
    void createReview_mismatchedShowId_throwsException() {
        insertOrderFixture(1L, 100L, 200L, 300L, "COMPLETED");
        insertShow(201L, "另一场演出");

        assertThatThrownBy(() -> reviewService.createReview(100L, createRequest(1L, 201L, 5, "演出不匹配")))
                .isInstanceOf(BizException.class)
                .hasMessage("订单与演出不匹配")
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_返回带用户名评价列表_when_查询演出评价")
    void listShowReviews_returnsWithUsername() {
        insertOrderFixture(1L, 100L, 200L, 300L, "COMPLETED");
        insertOrderFixture(2L, 101L, 200L, 301L, "COMPLETED");
        insertReview(1L, 1L, 100L, 200L, 5, "第一条评价", "2026-05-03 10:00:00");
        insertReview(2L, 2L, 101L, 200L, 4, "第二条评价", "2026-05-03 11:00:00");

        List<ReviewResponse> reviews = reviewService.listShowReviews(200L);

        assertThat(reviews).hasSize(2);
        assertThat(reviews).extracting(ReviewResponse::getUsername)
                .containsExactly("user101", "user100");
        assertThat(reviews).extracting(ReviewResponse::getContent)
                .containsExactly("第二条评价", "第一条评价");
    }

    @Test
    @DisplayName("should_按最新优先排序_when_查询演出评价")
    void listShowReviews_orderedByNewestFirst() {
        insertOrderFixture(1L, 100L, 200L, 300L, "COMPLETED");
        insertOrderFixture(2L, 101L, 200L, 301L, "COMPLETED");
        insertOrderFixture(3L, 102L, 200L, 302L, "COMPLETED");
        insertReview(1L, 1L, 100L, 200L, 5, "较早评价", "2026-05-03 09:00:00");
        insertReview(2L, 2L, 101L, 200L, 4, "最新评价", "2026-05-03 12:00:00");
        insertReview(3L, 3L, 102L, 200L, 3, "中间评价", "2026-05-03 10:00:00");

        List<ReviewResponse> reviews = reviewService.listShowReviews(200L);

        assertThat(reviews).extracting(ReviewResponse::getContent)
                .containsExactly("最新评价", "中间评价", "较早评价");
    }

    @Test
    @DisplayName("should_返回空列表_when_演出无评价")
    void listShowReviews_emptyShow_returnsEmptyList() {
        insertShow(200L, "暂无评价演出");

        List<ReviewResponse> reviews = reviewService.listShowReviews(200L);

        assertThat(reviews).isEmpty();
    }

    private ReviewCreateRequest createRequest(Long orderId, Long showId, Integer rating, String content) {
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setOrderId(orderId);
        request.setShowId(showId);
        request.setRating(rating);
        request.setContent(content);
        return request;
    }

    private void insertOrderFixture(Long orderId, Long userId, Long showId, Long ticketId, String payStatus) {
        insertUser(userId, "user" + userId);
        insertShow(showId, "测试演出" + showId);
        insertTicket(ticketId, showId);
        insertOrder(orderId, userId, showId, ticketId, payStatus);
    }

    private void insertUser(Long id, String username) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password, role, vip_level, points, status, is_deleted) " +
                        "VALUES (?, ?, ?, 'USER', 0, 0, 'ACTIVE', 0)",
                id, username, "password");
    }

    private void insertShow(Long id, String showName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shows WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO shows (id, show_name, show_time, venue, price_range, total_seats, status, is_deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'UPCOMING', 0)",
                id, showName, LocalDateTime.of(2026, 6, 1, 20, 0), "测试场馆", "100-500", 1000);
    }

    private void insertTicket(Long id, Long showId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tickets WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO tickets (id, show_id, ticket_type, price_type, seat_number, price, stock, sold) " +
                        "VALUES (?, ?, 'REGULAR', 'A区', ?, ?, 1, 1)",
                id, showId, "A-" + id, BigDecimal.valueOf(380));
    }

    private void insertOrder(Long id, Long userId, Long showId, Long ticketId, String payStatus) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, order_no, user_id, show_id, ticket_id, seat_number, amount, ticket_type, " +
                        "pay_status, order_time, pay_time) VALUES (?, ?, ?, ?, ?, ?, ?, 'REGULAR', ?, ?, ?)",
                id, "ORD" + id, userId, showId, ticketId, "A-" + ticketId, BigDecimal.valueOf(380), payStatus,
                LocalDateTime.of(2026, 5, 1, 10, 0), LocalDateTime.of(2026, 5, 1, 10, 5));
    }

    private void insertReview(Long id, Long orderId, Long userId, Long showId, Integer rating,
                              String content, String createdAt) {
        jdbcTemplate.update(
                "INSERT INTO reviews (id, order_id, user_id, show_id, content, rating, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, orderId, userId, showId, content, rating, createdAt);
    }
}
