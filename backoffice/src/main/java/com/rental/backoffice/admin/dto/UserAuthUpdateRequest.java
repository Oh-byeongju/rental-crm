package com.rental.backoffice.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 사용자 권한 미세 조정 일괄 저장 — ADR-009 §2-3.
 * 서버는 기존 CM_USER_AUTH 전체 삭제 → grants + revokes 재삽입.
 * 동일 AUTH 가 grants/revokes 양쪽에 있으면 거부.
 */
public record UserAuthUpdateRequest(
        @NotNull List<String> grants,
        @NotNull List<String> revokes
) {}
