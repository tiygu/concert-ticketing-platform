package com.concert.ticketing.user.controller;

import com.concert.ticketing.user.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("UserController 集成测试")
class UserControllerTest {

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

    private void insertUser(Long id, String username, String phone, String email, String status, int isDeleted) {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, password, role, phone, email, vip_level, points, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, username, "password", "USER", phone, email, 1, 100, status, isDeleted);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("should_返回分页用户列表且排除已删除用户_when_管理员查询用户列表")
    void should_return_paginated_user_list_and_exclude_deleted_users_when_admin_lists_users() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);
        insertUser(3L, "bob", "13800000002", "bob@test.com", "ACTIVE", 0);
        insertUser(4L, "deleted_user", "13800000003", "deleted@test.com", "ACTIVE", 1);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].username").value("bob"))
                .andExpect(jsonPath("$.data.records[0].password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("should_支持关键字搜索_when_管理员查询用户列表")
    void should_support_keyword_search_when_admin_lists_users() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);
        insertUser(3L, "bob", "13888888888", "bob@test.com", "ACTIVE", 0);
        insertUser(4L, "alice_deleted", "13800000003", "deleted@test.com", "ACTIVE", 1);

        mockMvc.perform(get("/api/admin/users")
                        .param("keyword", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("should_切换用户状态_when_管理员更新状态")
    void should_toggle_user_status_when_admin_updates_status() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);

        mockMvc.perform(put("/api/admin/users/2/status")
                        .contentType("application/json")
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        User updated = jdbcTemplate.queryForObject(
                "SELECT * FROM users WHERE id = 2",
                (rs, rowNum) -> {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setStatus(rs.getString("status"));
                    return user;
                });
        assertThat(updated.getStatus()).isEqualTo("DISABLED");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("should_返回错误_when_状态值非法")
    void should_return_error_when_status_value_is_invalid() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);

        mockMvc.perform(put("/api/admin/users/2/status")
                        .contentType("application/json")
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无效的状态值"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("should_返回当前用户资料_when_查询个人信息")
    void should_return_current_user_profile_when_getting_profile() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@test.com"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    @DisplayName("should_更新手机号和邮箱_when_修改个人资料")
    void should_update_phone_and_email_when_updating_profile() throws Exception {
        insertUser(2L, "alice", "13800000001", "alice@test.com", "ACTIVE", 0);

        mockMvc.perform(put("/api/user/profile")
                        .contentType("application/json")
                        .content("{\"phone\":\"13900000000\",\"email\":\"alice_new@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phone").value("13900000000"))
                .andExpect(jsonPath("$.data.email").value("alice_new@test.com"));

        User updated = jdbcTemplate.queryForObject(
                "SELECT * FROM users WHERE username = 'alice'",
                (rs, rowNum) -> {
                    User user = new User();
                    user.setPhone(rs.getString("phone"));
                    user.setEmail(rs.getString("email"));
                    return user;
                });
        assertThat(updated.getPhone()).isEqualTo("13900000000");
        assertThat(updated.getEmail()).isEqualTo("alice_new@test.com");
    }
}
