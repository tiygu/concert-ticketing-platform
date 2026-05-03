package com.concert.ticketing.vip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.vip.dto.VipBookingAuditRequest;
import com.concert.ticketing.vip.dto.VipBookingCreateRequest;
import com.concert.ticketing.vip.entity.VipBooking;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.mapper.VipBookingMapper;
import com.concert.ticketing.vip.mapper.VipPackageMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("VipBookingService 集成测试")
class VipBookingServiceTest {

    @Autowired
    private VipBookingService vipBookingService;

    @Autowired
    private VipPackageMapper vipPackageMapper;

    @Autowired
    private VipBookingMapper vipBookingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM vip_bookings");
        jdbcTemplate.execute("DELETE FROM vip_packages");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("should_创建待审核预约并增加已预约数_when_VIP等级和库存有效")
    void createBooking_success() {
        insertUser(100L);
        insertVipPackage(1L, "后台探班", "探班权益", 1, 2, 0, "ACTIVE");

        VipBooking booking = vipBookingService.createBooking(100L, 1, createRequest(1L));

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getPackageId()).isEqualTo(1L);
        assertThat(booking.getUserId()).isEqualTo(100L);
        assertThat(booking.getUseDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(booking.getAuditStatus()).isEqualTo("PENDING");
        assertThat(booking.getBookingTime()).isNotNull();

        VipPackage vipPackage = vipPackageMapper.selectById(1L);
        assertThat(vipPackage.getBookedCount()).isEqualTo(1);
        assertThat(vipBookingMapper.selectById(booking.getId()).getAuditStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("should_抛出404_when_预约套餐不存在")
    void createBooking_packageNotFound() {
        assertThatThrownBy(() -> vipBookingService.createBooking(100L, 1, createRequest(999L)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(404);
    }

    @Test
    @DisplayName("should_抛出400_when_预约套餐已下架")
    void createBooking_packageInactive() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 2, 0, "INACTIVE");

        assertThatThrownBy(() -> vipBookingService.createBooking(100L, 1, createRequest(1L)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_抛出403_when_VIP等级不足")
    void createBooking_insufficientVipLevel() {
        insertVipPackage(1L, "后台探班", "探班权益", 2, 2, 0, "ACTIVE");

        assertThatThrownBy(() -> vipBookingService.createBooking(100L, 1, createRequest(1L)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    @DisplayName("should_抛出400_when_预约名额已满")
    void createBooking_stockFull() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 1, 1, "ACTIVE");

        assertThatThrownBy(() -> vipBookingService.createBooking(100L, 1, createRequest(1L)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_抛出403_when_VIP等级为空")
    void createBooking_nullVipLevel() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 2, 0, "ACTIVE");

        assertThatThrownBy(() -> vipBookingService.createBooking(100L, null, createRequest(1L)))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(403);
    }

    @Test
    @DisplayName("should_只返回当前用户预约_when_查询用户预约列表")
    void getUserBookings_returnsOnlyUserBookings() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 5, 0, "ACTIVE");
        insertVipBooking(1L, 1L, 100L, "PENDING", "2026-05-03 10:00:00");
        insertVipBooking(2L, 1L, 101L, "PENDING", "2026-05-03 11:00:00");

        List<VipBooking> bookings = vipBookingService.getUserBookings(100L);

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getUserId()).isEqualTo(100L);
        assertThat(bookings.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should_分页并按审核状态筛选_when_后台查询预约")
    void getAllBookings_paginationAndFilter() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 5, 0, "ACTIVE");
        insertVipBooking(1L, 1L, 100L, "PENDING", "2026-05-03 10:00:00");
        insertVipBooking(2L, 1L, 101L, "APPROVED", "2026-05-03 11:00:00");
        insertVipBooking(3L, 1L, 102L, "APPROVED", "2026-05-03 12:00:00");
        insertVipBooking(4L, 1L, 103L, "APPROVED", "2026-05-03 13:00:00");

        IPage<VipBooking> bookingPage = vipBookingService.getAllBookings(1, 2, "APPROVED");

        assertThat(bookingPage.getTotal()).isEqualTo(3);
        assertThat(bookingPage.getCurrent()).isEqualTo(1);
        assertThat(bookingPage.getSize()).isEqualTo(2);
        assertThat(bookingPage.getRecords()).hasSize(2);
        assertThat(bookingPage.getRecords()).extracting(VipBooking::getAuditStatus)
                .containsOnly("APPROVED");
    }

    @Test
    @DisplayName("should_审核通过并保存回复_when_审核待处理预约")
    void auditBooking_approve() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 5, 0, "ACTIVE");
        insertVipBooking(1L, 1L, 100L, "PENDING", "2026-05-03 10:00:00");

        VipBooking booking = vipBookingService.auditBooking(1L, auditRequest("APPROVED", "审核通过"));

        assertThat(booking.getAuditStatus()).isEqualTo("APPROVED");
        assertThat(booking.getAdminReply()).isEqualTo("审核通过");
        assertThat(vipBookingMapper.selectById(1L).getAuditStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("should_审核驳回并保存回复_when_审核待处理预约")
    void auditBooking_reject() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 5, 0, "ACTIVE");
        insertVipBooking(1L, 1L, 100L, "PENDING", "2026-05-03 10:00:00");

        VipBooking booking = vipBookingService.auditBooking(1L, auditRequest("REJECTED", "名额冲突"));

        assertThat(booking.getAuditStatus()).isEqualTo("REJECTED");
        assertThat(booking.getAdminReply()).isEqualTo("名额冲突");
        assertThat(vipBookingMapper.selectById(1L).getAdminReply()).isEqualTo("名额冲突");
    }

    @Test
    @DisplayName("should_抛出400_when_审核状态非法")
    void auditBooking_invalidStatus() {
        assertThatThrownBy(() -> vipBookingService.auditBooking(1L, auditRequest("PENDING", "非法状态")))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    @DisplayName("should_抛出400_when_预约已审核")
    void auditBooking_alreadyAudited() {
        insertVipPackage(1L, "后台探班", "探班权益", 1, 5, 0, "ACTIVE");
        insertVipBooking(1L, 1L, 100L, "APPROVED", "2026-05-03 10:00:00");

        assertThatThrownBy(() -> vipBookingService.auditBooking(1L, auditRequest("REJECTED", "改为驳回")))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    private VipBookingCreateRequest createRequest(Long packageId) {
        VipBookingCreateRequest request = new VipBookingCreateRequest();
        request.setPackageId(packageId);
        request.setUseDate(LocalDate.of(2026, 6, 1));
        return request;
    }

    private VipBookingAuditRequest auditRequest(String auditStatus, String adminReply) {
        VipBookingAuditRequest request = new VipBookingAuditRequest();
        request.setAuditStatus(auditStatus);
        request.setAdminReply(adminReply);
        return request;
    }

    private void insertVipPackage(Long id, String packageName, String benefits, int userLevelRequired,
                                  int stock, int bookedCount, String status) {
        jdbcTemplate.update(
                "INSERT INTO vip_packages (id, package_name, benefits, usage_limit, valid_period, " +
                        "user_level_required, stock, booked_count, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, packageName, benefits, "限本人使用", "2026-12-31", userLevelRequired, stock, bookedCount, status);
    }

    private void insertVipBooking(Long id, Long packageId, Long userId, String auditStatus, String createdAt) {
        insertUser(userId);
        jdbcTemplate.update(
                "INSERT INTO vip_bookings (id, package_id, user_id, booking_time, use_date, audit_status, " +
                        "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, packageId, userId, createdAt, LocalDate.of(2026, 6, 1), auditStatus, createdAt, createdAt);
    }

    private void insertUser(Long id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password, role, vip_level, points, status, is_deleted) " +
                        "VALUES (?, ?, ?, 'USER', 1, 0, 'ACTIVE', 0)",
                id, "user" + id, "password");
    }
}
