package com.rental.crm.code.controller;

import com.rental.crm.code.dto.CodeCreateRequest;
import com.rental.crm.code.dto.CodeGroupCreateRequest;
import com.rental.crm.code.dto.CodeGroupResponse;
import com.rental.crm.code.dto.CodeGroupSearchRequest;
import com.rental.crm.code.dto.CodeGroupUpdateRequest;
import com.rental.crm.code.dto.CodeResponse;
import com.rental.crm.code.dto.CodeSearchRequest;
import com.rental.crm.code.dto.CodeUpdateRequest;
import com.rental.crm.code.service.CodeService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 공통코드 REST API — 07 API 명세서 §2.
 *  - /api/code-groups            : 그룹 CRUD
 *  - /api/code-groups/{g}/codes  : 그룹별 코드 검색
 *  - /api/code-groups/{g}/options: selectbox 옵션
 *  - /api/codes                  : 코드 등록/수정/비활성화
 */
@RestController
@RequiredArgsConstructor
public class CodeRestController {

    private final CodeService codeService;

    // ===================== 그룹 =====================

    @GetMapping("/api/code-groups")
    public ApiResponse<PageResponse<CodeGroupResponse>> searchGroups(
            @ModelAttribute CodeGroupSearchRequest search,
            @PageableDefault(size = 20, sort = "groupCode") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(codeService.searchGroups(search, pageable)));
    }

    @GetMapping("/api/code-groups/{groupCode}")
    public ApiResponse<CodeGroupResponse> findGroup(@PathVariable String groupCode) {
        return ApiResponse.ok(codeService.findGroup(groupCode));
    }

    @PostMapping("/api/code-groups")
    public ResponseEntity<ApiResponse<CodeGroupResponse>> registerGroup(
            @Valid @RequestBody CodeGroupCreateRequest req) {
        var created = codeService.registerGroup(req);
        return ResponseEntity
                .created(URI.create("/api/code-groups/" + created.groupCode()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/api/code-groups/{groupCode}")
    public ApiResponse<CodeGroupResponse> updateGroup(
            @PathVariable String groupCode,
            @Valid @RequestBody CodeGroupUpdateRequest req) {
        return ApiResponse.ok(codeService.updateGroup(groupCode, req), "수정되었습니다");
    }

    @DeleteMapping("/api/code-groups/{groupCode}")
    public ApiResponse<Void> deleteGroup(@PathVariable String groupCode) {
        codeService.deleteGroup(groupCode);
        return ApiResponse.ok(null, "삭제되었습니다");
    }

    // ===================== 코드값 =====================

    @GetMapping("/api/code-groups/{groupCode}/codes")
    public ApiResponse<PageResponse<CodeResponse>> searchCodes(
            @PathVariable String groupCode,
            @ModelAttribute CodeSearchRequest search,
            @PageableDefault(size = 20, sort = "sortOrder") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(codeService.searchCodes(groupCode, search, pageable)));
    }

    /** selectbox 옵션 — 사용중(Y) 만, sortOrder 순. */
    @GetMapping("/api/code-groups/{groupCode}/options")
    public ApiResponse<List<CodeResponse>> options(@PathVariable String groupCode) {
        return ApiResponse.ok(codeService.findActiveCodesByGroup(groupCode));
    }

    @GetMapping("/api/codes/{codeId}")
    public ApiResponse<CodeResponse> findCode(@PathVariable Long codeId) {
        return ApiResponse.ok(codeService.findCode(codeId));
    }

    @PostMapping("/api/codes")
    public ResponseEntity<ApiResponse<CodeResponse>> registerCode(
            @Valid @RequestBody CodeCreateRequest req) {
        var created = codeService.registerCode(req);
        return ResponseEntity
                .created(URI.create("/api/codes/" + created.codeId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/api/codes/{codeId}")
    public ApiResponse<CodeResponse> updateCode(
            @PathVariable Long codeId,
            @Valid @RequestBody CodeUpdateRequest req) {
        return ApiResponse.ok(codeService.updateCode(codeId, req), "수정되었습니다");
    }

    @DeleteMapping("/api/codes/{codeId}")
    public ApiResponse<Void> deactivateCode(@PathVariable Long codeId) {
        codeService.deactivateCode(codeId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }
}
