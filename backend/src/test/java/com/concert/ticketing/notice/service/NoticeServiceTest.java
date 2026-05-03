package com.concert.ticketing.notice.service;

import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.notice.dto.NoticeCreateRequest;
import com.concert.ticketing.notice.dto.NoticeUpdateRequest;
import com.concert.ticketing.notice.entity.Notice;
import com.concert.ticketing.notice.mapper.NoticeMapper;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DELETE FROM reviews",
        "DELETE FROM orders",
        "DELETE FROM tickets",
            "DELETE FROM vip_bookings",
            "DELETE FROM vip_packages",
        "DELETE FROM notices",
        "DELETE FROM shows",
        "DELETE FROM users"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED))
@Transactional
@DisplayName("NoticeService 集成测试")
class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private UserMapper userMapper;

    @Test
    @DisplayName("should_只返回已发布公告_when_查询公开公告列表")
    void should_return_only_published_notices_when_list_public_notices() {
        Long adminId = adminId();
        createNotice("草稿公告", "draft", null, adminId);
        createNotice("旧公告", "old", LocalDateTime.of(2026, 5, 3, 11, 0), adminId);
        createNotice("新公告", "new", LocalDateTime.of(2026, 5, 3, 12, 0), adminId);
        Notice archived = createNotice("归档公告", "archived", LocalDateTime.of(2026, 5, 3, 13, 0), adminId);
        noticeService.deleteNotice(archived.getId());

        List<Notice> notices = noticeService.listPublishedNotices();

        assertThat(notices).hasSize(2);
        assertThat(notices).extracting(Notice::getStatus)
                .containsOnly("PUBLISHED");
        assertThat(notices).extracting(Notice::getTitle)
                .containsExactly("新公告", "旧公告");
    }

    @Test
    @DisplayName("should_返回全部公告并按创建时间倒序_when_管理员查询公告列表")
    void should_return_all_notices_when_admin_lists_notices() throws InterruptedException {
        Long adminId = adminId();
        createNotice("最早", "a", null, adminId);
        Thread.sleep(10);
        createNotice("中间", "b", LocalDateTime.of(2026, 5, 3, 11, 0), adminId);
        Thread.sleep(10);
        Notice latest = createNotice("最新", "c", LocalDateTime.of(2026, 5, 3, 12, 0), adminId);
        noticeService.deleteNotice(latest.getId());

        List<Notice> notices = noticeService.listAllNotices();

        assertThat(notices).hasSize(3);
        assertThat(notices).extracting(Notice::getTitle)
                .containsExactly("最新", "中间", "最早");
    }

    @Test
    @DisplayName("should_设置发布者并默认草稿状态_when_创建公告未设置发布时间")
    void should_set_publisher_and_default_draft_status_when_create_notice() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("系统公告");
        request.setContent("平台维护通知");

        Notice notice = noticeService.createNotice(request, "admin");

        User admin = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", "admin"));
        assertThat(notice.getId()).isNotNull();
        assertThat(notice.getPublisherId()).isEqualTo(admin.getId());
        assertThat(notice.getStatus()).isEqualTo("DRAFT");
        assertThat(notice.getTitle()).isEqualTo("系统公告");
    }

    @Test
    @DisplayName("should_修改公告字段_when_更新公告")
    void should_update_notice_fields_when_update_notice() {
        Long adminId = adminId();
        Notice created = createNotice("旧标题", "旧内容", null, adminId);

        NoticeUpdateRequest request = new NoticeUpdateRequest();
        request.setTitle("新标题");
        request.setContent("新内容");
        request.setStatus("PUBLISHED");
        request.setPublishTime(LocalDateTime.of(2026, 5, 4, 9, 0));

        Notice notice = noticeService.updateNotice(created.getId(), request);

        assertThat(notice.getTitle()).isEqualTo("新标题");
        assertThat(notice.getContent()).isEqualTo("新内容");
        assertThat(notice.getStatus()).isEqualTo("PUBLISHED");
        assertThat(notice.getPublishTime()).isEqualTo(LocalDateTime.of(2026, 5, 4, 9, 0));
        assertThat(noticeMapper.selectById(created.getId()).getTitle()).isEqualTo("新标题");
    }

    @Test
    @DisplayName("should_归档公告_when_删除公告")
    void should_archive_notice_when_delete_notice() {
        Long adminId = adminId();
        Notice created = createNotice("待删除公告", "content", LocalDateTime.of(2026, 5, 3, 10, 0), adminId);

        noticeService.deleteNotice(created.getId());

        Notice notice = noticeMapper.selectById(created.getId());
        assertThat(notice.getStatus()).isEqualTo("ARCHIVED");
    }

    @Test
    @DisplayName("should_抛出异常_when_更新不存在的公告")
    void should_throw_exception_when_update_missing_notice() {
        NoticeUpdateRequest request = new NoticeUpdateRequest();
        request.setTitle("标题");

        assertThatThrownBy(() -> noticeService.updateNotice(999L, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("公告不存在");
    }

    private Long adminId() {
        User admin = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", "admin"));
        return admin.getId();
    }

    private Notice createNotice(String title, String content, LocalDateTime publishTime, Long adminId) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPublishTime(publishTime);
        notice.setStatus(publishTime == null ? "DRAFT" : "PUBLISHED");
        notice.setPublisherId(adminId);
        noticeMapper.insert(notice);
        return notice;
    }
}
