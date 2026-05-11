package com.rental.crm.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 역할-권한 매트릭스 저장 요청.
 * 서버는 기존 매핑 전체 삭제 → 새 authCodes 로 재삽입 (ADR-010 §2-2 invalidation 트리거).
 */
public record RoleAuthUpdateRequest(
        @NotNull List<String> authCodes
) {}
