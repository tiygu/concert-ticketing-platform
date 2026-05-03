package com.concert.ticketing.vip.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.common.dto.PageResult;
import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.user.entity.User;
import com.concert.ticketing.user.mapper.UserMapper;
import com.concert.ticketing.vip.dto.AdminVipBookingResponse;
import com.concert.ticketing.vip.dto.VipBookingAuditRequest;
import com.concert.ticketing.vip.dto.VipBookingCreateRequest;
import com.concert.ticketing.vip.dto.VipBookingResponse;
import com.concert.ticketing.vip.dto.VipPackageResponse;
import com.concert.ticketing.vip.entity.VipBooking;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.mapper.VipPackageMapper;
import com.concert.ticketing.vip.service.VipBookingService;
import com.concert.ticketing.vip.service.VipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class VipBookingController {

    private final VipPackageService vipPackageService;
    private final VipBookingService vipBookingService;
    private final VipPackageMapper vipPackageMapper;
    private final UserMapper userMapper;

    @GetMapping("/api/user/vip/packages")
    public Result<List<VipPackageResponse>> listAvailablePackages() {
        User user = getCurrentVipUser();
        List<VipPackageResponse> responses = vipPackageService.listAvailablePackages(user.getVipLevel()).stream()
                .map(VipPackageResponse::from)
                .collect(Collectors.toList());
        return Result.ok(responses);
    }

    @PostMapping("/api/user/vip/bookings")
    public Result<VipBookingResponse> createBooking(@Valid @RequestBody VipBookingCreateRequest request) {
        User user = getCurrentVipUser();
        VipBooking booking = vipBookingService.createBooking(user.getId(), user.getVipLevel(), request);
        VipPackage vipPackage = vipPackageMapper.selectById(booking.getPackageId());
        return Result.ok(VipBookingResponse.from(booking, vipPackage));
    }

    @GetMapping("/api/user/vip/bookings")
    public Result<List<VipBookingResponse>> listUserBookings() {
        User user = getCurrentVipUser();
        List<VipBookingResponse> responses = vipBookingService.getUserBookings(user.getId()).stream()
                .map(booking -> VipBookingResponse.from(booking, vipPackageMapper.selectById(booking.getPackageId())))
                .collect(Collectors.toList());
        return Result.ok(responses);
    }

    @GetMapping("/api/admin/vip/bookings")
    public Result<PageResult<AdminVipBookingResponse>> listAllBookings(@RequestParam(defaultValue = "1") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(required = false) String status) {
        IPage<VipBooking> bookingPage = vipBookingService.getAllBookings(page, size, status);
        Map<Long, User> userMap = getUserMap(bookingPage.getRecords());
        Map<Long, VipPackage> packageMap = getPackageMap(bookingPage.getRecords());
        List<AdminVipBookingResponse> records = bookingPage.getRecords().stream()
                .map(booking -> {
                    User user = userMap.get(booking.getUserId());
                    String username = user == null ? null : user.getUsername();
                    Integer userVipLevel = user == null ? null : user.getVipLevel();
                    return AdminVipBookingResponse.from(
                            booking,
                            packageMap.get(booking.getPackageId()),
                            username,
                            userVipLevel);
                })
                .collect(Collectors.toList());
        return Result.ok(new PageResult<>(records, bookingPage.getTotal(), page, size));
    }

    @PutMapping("/api/admin/vip/bookings/{id}/audit")
    public Result<AdminVipBookingResponse> auditBooking(@PathVariable Long id,
                                                        @Valid @RequestBody VipBookingAuditRequest request) {
        VipBooking booking = vipBookingService.auditBooking(id, request);
        VipPackage vipPackage = vipPackageMapper.selectById(booking.getPackageId());
        User user = userMapper.selectById(booking.getUserId());
        String username = user == null ? null : user.getUsername();
        Integer userVipLevel = user == null ? null : user.getVipLevel();
        return Result.ok(AdminVipBookingResponse.from(booking, vipPackage, username, userVipLevel));
    }

    private User getCurrentVipUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (user.getVipLevel() == null || user.getVipLevel() <= 0) {
            throw new BizException(403, "仅VIP用户可访问VIP权益");
        }
        return user;
    }

    private Map<Long, User> getUserMap(List<VipBooking> bookings) {
        Set<Long> userIds = bookings.stream()
                .map(VipBooking::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, VipPackage> getPackageMap(List<VipBooking> bookings) {
        Set<Long> packageIds = bookings.stream()
                .map(VipBooking::getPackageId)
                .collect(Collectors.toSet());
        if (packageIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vipPackageMapper.selectList(new LambdaQueryWrapper<VipPackage>().in(VipPackage::getId, packageIds)).stream()
                .collect(Collectors.toMap(VipPackage::getId, Function.identity()));
    }
}
