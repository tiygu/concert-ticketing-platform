package com.concert.ticketing.vip.controller;

import com.concert.ticketing.common.dto.Result;
import com.concert.ticketing.vip.dto.VipPackageCreateRequest;
import com.concert.ticketing.vip.dto.VipPackageResponse;
import com.concert.ticketing.vip.dto.VipPackageUpdateRequest;
import com.concert.ticketing.vip.entity.VipPackage;
import com.concert.ticketing.vip.service.VipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vip/packages")
public class VipPackageController {

    private final VipPackageService vipPackageService;

    @GetMapping
    public Result<List<VipPackageResponse>> listPackages() {
        List<VipPackageResponse> responses = vipPackageService.listPackages().stream()
                .map(VipPackageResponse::from)
                .collect(Collectors.toList());
        return Result.ok(responses);
    }

    @PostMapping
    public Result<VipPackageResponse> createPackage(@Valid @RequestBody VipPackageCreateRequest request) {
        VipPackage vipPackage = vipPackageService.createPackage(request);
        return Result.ok(VipPackageResponse.from(vipPackage));
    }

    @PutMapping("/{id}")
    public Result<VipPackageResponse> updatePackage(@PathVariable Long id,
                                                    @Valid @RequestBody VipPackageUpdateRequest request) {
        VipPackage vipPackage = vipPackageService.updatePackage(id, request);
        return Result.ok(VipPackageResponse.from(vipPackage));
    }

    @DeleteMapping("/{id}")
    public Result<Void> disablePackage(@PathVariable Long id) {
        vipPackageService.disablePackage(id);
        return Result.ok();
    }
}
