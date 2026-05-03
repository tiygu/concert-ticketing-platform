package com.concert.ticketing.vip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.vip.dto.VipBookingAuditRequest;
import com.concert.ticketing.vip.dto.VipBookingCreateRequest;
import com.concert.ticketing.vip.entity.VipBooking;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.mapper.VipBookingMapper;
import com.concert.ticketing.vip.mapper.VipPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipBookingServiceImpl implements VipBookingService {

    private static final String PACKAGE_STATUS_ACTIVE = "ACTIVE";
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final String AUDIT_STATUS_APPROVED = "APPROVED";
    private static final String AUDIT_STATUS_REJECTED = "REJECTED";

    private final VipPackageMapper vipPackageMapper;
    private final VipBookingMapper vipBookingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipBooking createBooking(Long userId, Integer vipLevel, VipBookingCreateRequest request) {
        VipPackage vipPackage = vipPackageMapper.selectById(request.getPackageId());
        if (vipPackage == null) {
            throw new BizException(404, "VIP套餐不存在");
        }
        if (!PACKAGE_STATUS_ACTIVE.equals(vipPackage.getStatus())) {
            throw new BizException(400, "VIP套餐已下架");
        }
        if (vipLevel == null || vipLevel < vipPackage.getUserLevelRequired()) {
            throw new BizException(403, "VIP等级不足，无法预约该权益");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<VipPackage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VipPackage::getId, request.getPackageId())
                .eq(VipPackage::getStatus, PACKAGE_STATUS_ACTIVE)
                .apply("booked_count < stock")
                .setSql("booked_count = booked_count + 1")
                .set(VipPackage::getUpdatedAt, now);

        boolean updated = vipPackageMapper.update(null, updateWrapper) > 0;
        if (!updated) {
            VipPackage recheck = vipPackageMapper.selectById(request.getPackageId());
            if (recheck == null || !PACKAGE_STATUS_ACTIVE.equals(recheck.getStatus())) {
                throw new BizException(400, "VIP套餐已下架");
            }
            throw new BizException(400, "VIP权益套餐名额已满");
        }

        VipBooking booking = new VipBooking();
        booking.setPackageId(request.getPackageId());
        booking.setUserId(userId);
        booking.setBookingTime(now);
        booking.setUseDate(request.getUseDate());
        booking.setAuditStatus(AUDIT_STATUS_PENDING);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        vipBookingMapper.insert(booking);

        log.info("VIP booking created: bookingId={}, packageId={}, userId={}",
                booking.getId(), request.getPackageId(), userId);
        return vipBookingMapper.selectById(booking.getId());
    }

    @Override
    public java.util.List<VipBooking> getUserBookings(Long userId) {
        return vipBookingMapper.selectList(
                new LambdaQueryWrapper<VipBooking>()
                        .eq(VipBooking::getUserId, userId)
                        .orderByDesc(VipBooking::getCreatedAt)
        );
    }

    @Override
    public IPage<VipBooking> getAllBookings(Integer page, Integer pageSize, String auditStatus) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<VipBooking> countWrapper = new LambdaQueryWrapper<>();
        addAuditStatusFilter(countWrapper, auditStatus);
        long total = vipBookingMapper.selectCount(countWrapper);

        LambdaQueryWrapper<VipBooking> queryWrapper = new LambdaQueryWrapper<>();
        addAuditStatusFilter(queryWrapper, auditStatus);
        long offset = (long) (pageNo - 1) * size;
        queryWrapper.orderByDesc(VipBooking::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + offset);

        Page<VipBooking> bookingPage = vipBookingMapper.selectPage(new Page<>(pageNo, size), queryWrapper);
        bookingPage.setTotal(total);
        return bookingPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipBooking auditBooking(Long bookingId, VipBookingAuditRequest request) {
        String auditStatus = normalizeAuditStatus(request.getAuditStatus());
        if (!AUDIT_STATUS_APPROVED.equals(auditStatus) && !AUDIT_STATUS_REJECTED.equals(auditStatus)) {
            throw new BizException(400, "审核状态仅支持APPROVED或REJECTED");
        }

        VipBooking booking = vipBookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new BizException(404, "VIP预约不存在");
        }
        if (!AUDIT_STATUS_PENDING.equals(booking.getAuditStatus())) {
            throw new BizException(400, "只有待审核预约可以审核");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<VipBooking> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VipBooking::getId, bookingId)
                .eq(VipBooking::getAuditStatus, AUDIT_STATUS_PENDING)
                .set(VipBooking::getAuditStatus, auditStatus)
                .set(VipBooking::getAdminReply, request.getAdminReply())
                .set(VipBooking::getUpdatedAt, now);

        boolean updated = vipBookingMapper.update(null, updateWrapper) > 0;
        if (!updated) {
            throw new BizException(409, "预约状态已变更，请刷新后重试");
        }

        VipBooking auditedBooking = vipBookingMapper.selectById(bookingId);
        log.info("VIP booking audited: bookingId={}, auditStatus={}", bookingId, auditStatus);
        return auditedBooking;
    }

    private void addAuditStatusFilter(LambdaQueryWrapper<VipBooking> wrapper, String auditStatus) {
        if (auditStatus != null && !auditStatus.isBlank()) {
            wrapper.eq(VipBooking::getAuditStatus, auditStatus.trim());
        }
    }

    private String normalizeAuditStatus(String auditStatus) {
        if (auditStatus == null) {
            return null;
        }
        return auditStatus.trim().toUpperCase(Locale.ROOT);
    }
}
