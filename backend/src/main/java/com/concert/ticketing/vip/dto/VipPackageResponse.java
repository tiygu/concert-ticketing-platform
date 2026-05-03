package com.concert.ticketing.vip.dto;

import com.concert.ticketing.vip.entity.VipPackage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VipPackageResponse {

    private Long id;
    private String packageName;
    private String benefits;
    private String usageLimit;
    private String validPeriod;
    private Integer userLevelRequired;
    private Integer stock;
    private Integer bookedCount;
    private String status;
    private String statusText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VipPackageResponse from(VipPackage p) {
        VipPackageResponse response = new VipPackageResponse();
        response.setId(p.getId());
        response.setPackageName(p.getPackageName());
        response.setBenefits(p.getBenefits());
        response.setUsageLimit(p.getUsageLimit());
        response.setValidPeriod(p.getValidPeriod());
        response.setUserLevelRequired(p.getUserLevelRequired());
        response.setStock(p.getStock());
        response.setBookedCount(p.getBookedCount());
        response.setStatus(p.getStatus());
        response.setStatusText(resolveStatusText(p.getStatus()));
        response.setCreatedAt(p.getCreatedAt());
        response.setUpdatedAt(p.getUpdatedAt());
        return response;
    }

    private static String resolveStatusText(String status) {
        if (status == null) {
            return null;
        }
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return "上架中";
        }
        if ("INACTIVE".equalsIgnoreCase(status)) {
            return "已下架";
        }
        return status;
    }
}
