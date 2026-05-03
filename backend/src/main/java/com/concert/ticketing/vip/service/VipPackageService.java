package com.concert.ticketing.vip.service;

import com.concert.ticketing.vip.dto.VipPackageCreateRequest;
import com.concert.ticketing.vip.dto.VipPackageUpdateRequest;
import com.concert.ticketing.vip.entity.VipPackage;

import java.util.List;

public interface VipPackageService {

    List<VipPackage> listPackages();

    VipPackage createPackage(VipPackageCreateRequest req);

    VipPackage updatePackage(Long id, VipPackageUpdateRequest req);

    void disablePackage(Long id);
}
