package com.rental.crm.auth.controller;

import com.rental.crm.auth.dto.RoleAuthUpdateRequest;
import com.rental.crm.auth.dto.RoleCreateRequest;
import com.rental.crm.auth.dto.RoleResponse;
import com.rental.crm.auth.dto.RoleSearchRequest;
import com.rental.crm.auth.dto.RoleUpdateRequest;
import com.rental.crm.auth.service.RoleService;
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
import java.util.List;

/**
 * 역할 + 역할-권한 매핑 REST API — 07 §4.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleRestController {

    private final RoleService roleService;

    @GetMapping
    public ApiResponse<PageResponse<RoleResponse>> search(
            @ModelAttribute RoleSearchRequest search,
            @PageableDefault(size = 20, sort = "roleId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(roleService.search(search, pageable)));
    }

    @GetMapping("/{roleId}")
    public ApiResponse<RoleResponse> detail(@PathVariable Long roleId) {
        return ApiResponse.ok(roleService.findById(roleId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> register(
            @Valid @RequestBody RoleCreateRequest req) {
        var created = roleService.register(req);
        return ResponseEntity
                .created(URI.create("/api/roles/" + created.roleId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{roleId}")
    public ApiResponse<RoleResponse> update(@PathVariable Long roleId,
                                            @Valid @RequestBody RoleUpdateRequest req) {
        return ApiResponse.ok(roleService.update(roleId, req), "수정되었습니다");
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> delete(@PathVariable Long roleId) {
        roleService.delete(roleId);
        return ApiResponse.ok(null, "삭제되었습니다");
    }

    /** 역할에 부여된 AUTH_CODE 목록 — 매트릭스 초기 체크 상태. */
    @GetMapping("/{roleId}/auths")
    public ApiResponse<List<String>> findAuths(@PathVariable Long roleId) {
        return ApiResponse.ok(roleService.findAuthCodes(roleId));
    }

    /** 매트릭스 일괄 저장 — 전체 삭제 후 재삽입. */
    @PutMapping("/{roleId}/auths")
    public ApiResponse<Void> updateAuths(@PathVariable Long roleId,
                                         @Valid @RequestBody RoleAuthUpdateRequest req) {
        roleService.updateRoleAuths(roleId, req);
        return ApiResponse.ok(null, "권한이 저장되었습니다");
    }
}
