package com.rental.domain.admin.entity;

import com.rental.domain.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 직접 권한 매핑 — `CM_USER_AUTH` (ADR-009).
 * 역할 권한에 추가(`GRANT`) 또는 역할 권한에서 제외(`REVOKE`).
 */
@Entity
@Table(name = "CM_USER_AUTH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth extends BaseAuditEntity {

    public static final String TYPE_GRANT  = "GRANT";
    public static final String TYPE_REVOKE = "REVOKE";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_user_auth")
    @SequenceGenerator(name = "seq_cm_user_auth", sequenceName = "SEQ_CM_USER_AUTH",
                       allocationSize = 50)
    @Column(name = "USER_AUTH_ID")
    private Long userAuthId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "AUTH_CODE", length = 50, nullable = false)
    private String authCode;

    @Column(name = "GRANT_TYPE", length = 10, nullable = false)
    private String grantType;

    @Builder
    private UserAuth(Long userId, String authCode, String grantType) {
        this.userId    = userId;
        this.authCode  = authCode;
        this.grantType = grantType;
    }
}
