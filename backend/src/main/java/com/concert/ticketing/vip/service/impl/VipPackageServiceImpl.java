package com.concert.ticketing.vip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concert.ticketing.common.exception.BizException;
import com.concert.ticketing.vip.dto.VipPackageCreateRequest;
import com.concert.ticketing.vip.dto.VipPackageUpdateRequest;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.mapper.VipPackageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipPackageServiceImpl implements VipPackageService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";

    private final VipPackageMapper vipPackageMapper;

    @Override
    public List<VipPackage> listPackages() {
        return vipPackageMapper.selectList(new LambdaQueryWrapper<VipPackage>().orderByDesc(VipPackage::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipPackage createPackage(VipPackageCreateRequest req) {
        VipPackage vipPackage = new VipPackage();
        LocalDateTime now = LocalDateTime.now();
        vipPackage.setPackageName(req.getPackageName());
        vipPackage.setBenefits(req.getBenefits());
        vipPackage.setUsageLimit(req.getUsageLimit());
        vipPackage.setValidPeriod(req.getValidPeriod());
        vipPackage.setUserLevelRequired(req.getUserLevelRequired());
        vipPackage.setStock(req.getStock());
        vipPackage.setBookedCount(0);
        vipPackage.setStatus(ACTIVE_STATUS);
        vipPackage.setCreatedAt(now);
        vipPackage.setUpdatedAt(now);
        vipPackageMapper.insert(vipPackage);
        return getPackageById(vipPackage.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipPackage updatePackage(Long id, VipPackageUpdateRequest req) {
        VipPackage vipPackage = getPackageById(id);
        if (req.getPackageName() != null) {
            vipPackage.setPackageName(req.getPackageName());
        }
        if (req.getBenefits() != null) {
            vipPackage.setBenefits(req.getBenefits());
        }
        if (req.getUsageLimit() != null) {
            vipPackage.setUsageLimit(req.getUsageLimit());
        }
        if (req.getValidPeriod() != null) {
            vipPackage.setValidPeriod(req.getValidPeriod());
        }
        if (req.getUserLevelRequired() != null) {
            vipPackage.setUserLevelRequired(req.getUserLevelRequired());
        }
        if (req.getStock() != null) {
            vipPackage.setStock(req.getStock());
        }
        if (req.getStatus() != null) {
            vipPackage.setStatus(req.getStatus());
        }
        vipPackage.setUpdatedAt(LocalDateTime.now());
        vipPackageMapper.updateById(vipPackage);
        return getPackageById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disablePackage(Long id) {
        getPackageById(id);
        VipPackage vipPackage = new VipPackage();
        vipPackage.setId(id);
        vipPackage.setStatus(INACTIVE_STATUS);
        vipPackage.setUpdatedAt(LocalDateTime.now());
        vipPackageMapper.updateById(vipPackage);
    }

    private VipPackage getPackageById(Long id) {
        VipPackage vipPackage = vipPackageMapper.selectById(id);
        if (vipPackage == null) {
            throw new BizException(404, "VIP套餐不存在");
        }
        return vipPackage;
    }
}
