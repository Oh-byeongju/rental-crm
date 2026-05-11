package com.rental.crm.menu.controller;

import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import com.rental.crm.menu.dto.MenuCreateRequest;
import com.rental.crm.menu.dto.MenuResponse;
import com.rental.crm.menu.dto.MenuSearchRequest;
import com.rental.crm.menu.dto.MenuTreeNode;
import com.rental.crm.menu.dto.MenuUpdateRequest;
import com.rental.crm.menu.service.MenuService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 메뉴 REST API — 07 §4.
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuRestController {

    private final MenuService menuService;

    /** 전체 메뉴 트리. useYn 필터 (기본: null=전체, "Y"=사용중만 — 사이드바용). */
    @GetMapping
    public ApiResponse<List<MenuTreeNode>> tree(@RequestParam(required = false) String useYn) {
        return ApiResponse.ok(menuService.findMenuTree(useYn));
    }

    /** 평탄 검색 페이징 — 메뉴 관리 그리드. */
    @GetMapping("/flat")
    public ApiResponse<PageResponse<MenuResponse>> searchFlat(
            @ModelAttribute MenuSearchRequest search,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(menuService.search(search, pageable)));
    }

    /** PARENT_MENU_ID selectbox 옵션 — 사용중 GROUP 만. */
    @GetMapping("/groups")
    public ApiResponse<List<MenuResponse>> groups() {
        return ApiResponse.ok(menuService.findActiveGroups());
    }

    @GetMapping("/{menuId}")
    public ApiResponse<MenuResponse> detail(@PathVariable Long menuId) {
        return ApiResponse.ok(menuService.findById(menuId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> register(@Valid @RequestBody MenuCreateRequest req) {
        var created = menuService.register(req);
        return ResponseEntity
                .created(URI.create("/api/menus/" + created.menuId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{menuId}")
    public ApiResponse<MenuResponse> update(@PathVariable Long menuId,
                                            @Valid @RequestBody MenuUpdateRequest req) {
        return ApiResponse.ok(menuService.update(menuId, req), "수정되었습니다");
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> delete(@PathVariable Long menuId) {
        menuService.delete(menuId);
        return ApiResponse.ok(null, "삭제되었습니다");
    }
}
