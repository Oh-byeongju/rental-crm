package com.rental.backoffice.auth.controller;

import com.rental.backoffice.auth.dto.AuthResponse;
import com.rental.backoffice.auth.service.AuthService;
import com.rental.domain.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 권한 키 마스터 조회 — 07 §4. 매트릭스 화면이 사용.
 * 본 사이클에서는 GET 만 (CRUD 화면 없음, 시드만).
 */
@RestController
@RequestMapping("/api/auths")
@RequiredArgsConstructor
public class AuthRestController {

    private final AuthService authService;

    /** 매트릭스 화면용 — 사용중 AUTH 전체 + 메뉴명. */
    @GetMapping
    public ApiResponse<List<AuthResponse>> all() {
        return ApiResponse.ok(authService.findAllForMatrix());
    }
}
