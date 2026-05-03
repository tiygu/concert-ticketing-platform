package com.concert.ticketing.vip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.concert.ticketing.vip.dto.VipBookingAuditRequest;
import com.concert.ticketing.vip.dto.VipBookingCreateRequest;
import com.concert.ticketing.vip.entity.VipBooking;

import java.util.List;

public interface VipBookingService {

    VipBooking createBooking(Long userId, Integer vipLevel, VipBookingCreateRequest request);

    List<VipBooking> getUserBookings(Long userId);

    IPage<VipBooking> getAllBookings(Integer page, Integer pageSize, String auditStatus);

    VipBooking auditBooking(Long bookingId, VipBookingAuditRequest request);
}
