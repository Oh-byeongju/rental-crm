package com.rental.backoffice.auth.dto;

import com.rental.domain.auth.entity.Auth;

/**
 * 권한 키 마스터 응답 — 매트릭스 화면용 (메뉴명 포함).
 */
public record AuthResponse(
        String authCode,
        String authName,
        Long menuId,
        String menuName,
        String authType,
        Integer sortOrder,
        String useYn
) {
    public static AuthResponse from(Auth a, String menuName) {
        return new AuthResponse(
                a.getAuthCode(),
                a.getAuthName(),
                a.getMenuId(),
                menuName,
                a.getAuthType(),
                a.getSortOrder(),
                a.getUseYn()
        );
    }
}
