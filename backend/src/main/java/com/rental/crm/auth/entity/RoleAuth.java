package com.rental.crm.auth.entity;

import com.rental.crm.common.entity.BaseAuditEntity;
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
 * 역할-권한 매핑 — `CM_ROLE_AUTH` (ADR-008).
 * UNIQUE (ROLE_ID, AUTH_CODE).
 */
@Entity
@Table(name = "CM_ROLE_AUTH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleAuth extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_role_auth")
    @SequenceGenerator(name = "seq_cm_role_auth", sequenceName = "SEQ_CM_ROLE_AUTH",
                       allocationSize = 50)
    @Column(name = "ROLE_AUTH_ID")
    private Long roleAuthId;

    @Column(name = "ROLE_ID", nullable = false)
    private Long roleId;

    @Column(name = "AUTH_CODE", length = 50, nullable = false)
    private String authCode;

    @Builder
    private RoleAuth(Long roleId, String authCode) {
        this.roleId   = roleId;
        this.authCode = authCode;
    }
}
