package com.concert.ticketing.vip.service;

import com.concert.ticketing.vip.dto.VipPackageCreateRequest;
import com.concert.ticketing.vip.dto.VipPackageUpdateRequest;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.mapper.VipPackageMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/db/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("VipPackageService 单元测试")
class VipPackageServiceTest {

    @Autowired
    private VipPackageService vipPackageService;

    @Autowired
    private VipPackageMapper vipPackageMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM vip_bookings");
        jdbcTemplate.execute("DELETE FROM vip_packages");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private void insertVipPackage(Long id, String packageName, String benefits, String usageLimit,
                                  String validPeriod, int userLevelRequired, int stock,
                                  int bookedCount, String status) {
        jdbcTemplate.update(
                "INSERT INTO vip_packages (id, package_name, benefits, usage_limit, valid_period, user_level_required, stock, booked_count, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, packageName, benefits, usageLimit, validPeriod, userLevelRequired, stock, bookedCount, status);
    }

    @Test
    @DisplayName("should_返回所有套餐_when_查询套餐列表")
    void should_return_all_packages_when_list_packages() {
        insertVipPackage(1L, "尊享套餐A", "权益A", "限本人使用", "2026-12-31", 1, 10, 0, "ACTIVE");
        insertVipPackage(2L, "尊享套餐B", "权益B", "限本人使用", "2026-12-31", 2, 5, 1, "INACTIVE");

        List<VipPackage> packages = vipPackageService.listPackages();

        assertThat(packages).hasSize(2);
        assertThat(packages).extracting(VipPackage::getPackageName)
                .containsExactlyInAnyOrder("尊享套餐A", "尊享套餐B");
    }

    @Test
    @DisplayName("should_默认已预约数量和状态_when_创建套餐")
    void should_default_booked_count_and_status_when_create_package() {
        VipPackageCreateRequest request = new VipPackageCreateRequest();
        request.setPackageName("尊享套餐");
        request.setBenefits("后台探班");
        request.setUsageLimit("限本人使用");
        request.setValidPeriod("2026-12-31");
        request.setUserLevelRequired(1);
        request.setStock(12);

        VipPackage vipPackage = vipPackageService.createPackage(request);

        assertThat(vipPackage.getId()).isNotNull();
        assertThat(vipPackage.getBookedCount()).isEqualTo(0);
        assertThat(vipPackage.getStatus()).isEqualTo("ACTIVE");
        assertThat(vipPackageMapper.selectById(vipPackage.getId()).getBookedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should_更新可编辑字段_when_修改套餐")
    void should_update_editable_fields_when_update_package() {
        insertVipPackage(1L, "尊享套餐", "权益A", "限本人使用", "2026-12-31", 1, 10, 0, "ACTIVE");

        VipPackageUpdateRequest request = new VipPackageUpdateRequest();
        request.setPackageName("尊享套餐Plus");
        request.setBenefits("权益B");
        request.setStock(20);
        request.setStatus("INACTIVE");

        VipPackage updated = vipPackageService.updatePackage(1L, request);

        assertThat(updated.getPackageName()).isEqualTo("尊享套餐Plus");
        assertThat(updated.getBenefits()).isEqualTo("权益B");
        assertThat(updated.getStock()).isEqualTo(20);
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("should_仅下架不删除记录_when_禁用套餐")
    void should_disable_without_physical_delete_when_disable_package() {
        insertVipPackage(1L, "尊享套餐", "权益A", "限本人使用", "2026-12-31", 1, 10, 0, "ACTIVE");

        vipPackageService.disablePackage(1L);

        VipPackage vipPackage = vipPackageMapper.selectById(1L);
        assertThat(vipPackage).isNotNull();
        assertThat(vipPackage.getStatus()).isEqualTo("INACTIVE");
    }
}
