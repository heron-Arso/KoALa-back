package com.koala.koalaback.api.admin;

import com.koala.koalaback.domain.admin.dto.AdminDto;
import com.koala.koalaback.domain.admin.entity.AdminAuditLog;
import com.koala.koalaback.domain.admin.service.AdminService;
import com.koala.koalaback.global.response.ApiResponse;
import com.koala.koalaback.global.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/api/v1")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/auth/login")
    public ApiResponse<AdminDto.TokenResponse> login(
            @Valid @RequestBody AdminDto.LoginRequest req,
            HttpServletRequest httpReq) {
        return ApiResponse.ok(adminService.login(req, httpReq));
    }

    @GetMapping("/me")
    public ApiResponse<AdminDto.AdminResponse> getMyInfo(
            @AuthenticationPrincipal Long adminId) {
        return ApiResponse.ok(adminService.getMyInfo(adminId));
    }

    @PostMapping("/skus/stock-adjust")
    public ApiResponse<Void> adjustStock(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody AdminDto.StockAdjustRequest req,
            HttpServletRequest httpReq) {
        adminService.adjustStock(adminId, req, httpReq);
        return ApiResponse.ok();
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AdminAuditLog>> getAuditLogs(
            @AuthenticationPrincipal Long adminId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.getAuditLogs(adminId, pageable));
    }
}