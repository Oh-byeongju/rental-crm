package com.rental.crm.admin.dto;

import java.util.List;

/**
 * 권한 미세 조정 화면 응답 — ADR-009.
 *
 * <p>최종 권한 = roleAuths ∪ userGrants − userRevokes
 */
public record UserAuthMatrixResponse(
        /** 역할이 가진 AUTH 키 (사용자 GRANT/REVOKE 적용 전). */
        List<String> roleAuths,
        /** 사용자 직접 GRANT (역할에 없는데 추가 부여한 AUTH). */
        List<String> userGrants,
        /** 사용자 직접 REVOKE (역할에 있는데 빼앗은 AUTH). */
        List<String> userRevokes,
        /** 최종 권한 (캐시 키 기반). */
        List<String> effective
) {}
