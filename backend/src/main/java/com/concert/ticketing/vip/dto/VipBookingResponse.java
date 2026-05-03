package com.concert.ticketing.vip.dto;

import com.concert.ticketing.vip.entity.VipBooking;
import com.concert.ticketing.vip.entity.VipPackage;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VipBookingResponse {

    private Long id;
    private Long packageId;
    private String packageName;
    private String benefits;
    private Long userId;
    private LocalDateTime bookingTime;
    private LocalDate useDate;
    private String auditStatus;
    private String auditStatusText;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VipBookingResponse from(VipBooking booking, VipPackage vipPackage) {
        VipBookingResponse response = new VipBookingResponse();
        response.setId(booking.getId());
        response.setPackageId(booking.getPackageId());
        if (vipPackage != null) {
            response.setPackageName(vipPackage.getPackageName());
            response.setBenefits(vipPackage.getBenefits());
        }
        response.setUserId(booking.getUserId());
        response.setBookingTime(booking.getBookingTime());
        response.setUseDate(booking.getUseDate());
        response.setAuditStatus(booking.getAuditStatus());
        response.setAuditStatusText(resolveAuditStatusText(booking.getAuditStatus()));
        response.setAdminReply(booking.getAdminReply());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        return response;
    }

    private static String resolveAuditStatusText(String auditStatus) {
        if (auditStatus == null) {
            return null;
        }
        switch (auditStatus) {
            case "PENDING":
                return "待审核";
            case "APPROVED":
                return "已通过";
            case "REJECTED":
                return "已驳回";
            default:
                return auditStatus;
        }
    }
}
