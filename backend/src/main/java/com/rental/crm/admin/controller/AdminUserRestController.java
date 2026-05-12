package com.rental.crm.admin.controller;

import com.rental.crm.admin.dto.AdminUserCreateRequest;
import com.rental.crm.admin.dto.AdminUserPasswordResetRequest;
import com.rental.crm.admin.dto.AdminUserResponse;
import com.rental.crm.admin.dto.AdminUserSearchRequest;
import com.rental.crm.admin.dto.AdminUserUpdateRequest;
import com.rental.crm.admin.dto.UserAuthMatrixResponse;
import com.rental.crm.admin.dto.UserAuthUpdateRequest;
import com.rental.crm.admin.service.AdminUserService;
import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 관리자 REST API — 07 §3 + 권한 미세 조정 (ADR-009).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AdminUserRestController {

    private final AdminUserService userService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> search(
            @ModelAttribute AdminUserSearchRequest search,
            @PageableDefault(size = 20, sort = "userId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(userService.search(search, pageable)));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long userId) {
        return ApiResponse.ok(userService.findById(userId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserResponse>> register(
            @Valid @RequestBody AdminUserCreateRequest req) {
        var created = userService.register(req);
        return ResponseEntity
                .created(URI.create("/api/users/" + created.userId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{userId}")
    public ApiResponse<AdminUserResponse> update(@PathVariable Long userId,
                                                 @Valid @RequestBody AdminUserUpdateRequest req) {
        return ApiResponse.ok(userService.update(userId, req), "수정되었습니다");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deactivate(@PathVariable Long userId) {
        userService.deactivate(userId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }

    @PutMapping("/{userId}/unlock")
    public ApiResponse<Void> unlock(@PathVariable Long userId) {
        userService.unlock(userId);
        return ApiResponse.ok(null, "잠금이 해제되었습니다");
    }

    @PutMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId,
                                           @Valid @RequestBody AdminUserPasswordResetRequest req) {
        userService.resetPassword(userId, req);
        return ApiResponse.ok(null, "비밀번호가 재설정되었습니다");
    }

    // ===================== 권한 미세 조정 (ADR-009) =====================

    @GetMapping("/{userId}/auths")
    public ApiResponse<UserAuthMatrixResponse> matrix(@PathVariable Long userId) {
        return ApiResponse.ok(userService.findMatrix(userId));
    }

    @PutMapping("/{userId}/auths")
    public ApiResponse<Void> updateAuths(@PathVariable Long userId,
                                         @Valid @RequestBody UserAuthUpdateRequest req) {
        userService.updateUserAuths(userId, req);
        return ApiResponse.ok(null, "권한이 저장되었습니다");
    }
}
