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
 * 역할 — `CM_ROLE`.
 * `SUPER_ADMIN` 은 수정/삭제 불가 (04 §1-3, ADR-008).
 */
@Entity
@Table(name = "CM_ROLE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends BaseAuditEntity {

    public static final String CODE_SUPER_ADMIN = "SUPER_ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_role")
    @SequenceGenerator(name = "seq_cm_role", sequenceName = "SEQ_CM_ROLE", allocationSize = 50)
    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "ROLE_CODE", length = 50, nullable = false, unique = true)
    private String roleCode;

    @Column(name = "ROLE_NAME", length = 100, nullable = false)
    private String roleName;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Role(String roleCode, String roleName, String description) {
        this.roleCode    = roleCode;
        this.roleName    = roleName;
        this.description = description;
        this.useYn       = "Y";
    }

    public void changeName(String roleName) {
        this.roleName = roleName;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeUseYn(String useYn) {
        this.useYn = useYn;
    }

    public boolean isSuperAdmin() {
        return CODE_SUPER_ADMIN.equals(this.roleCode);
    }
}
