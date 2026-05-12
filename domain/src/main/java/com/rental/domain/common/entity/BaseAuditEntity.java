package com.rental.domain.common.entity;

import com.rental.domain.common.audit.AuditContext;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 도메인 엔티티의 부모. ADR-001 의 공통 9컬럼을 자동 주입.
 *
 * <p>주입 분담:
 * <ul>
 *   <li>시간 (FIRS/FINA_REG_DTS): JPA Auditing @CreatedDate / @LastModifiedDate</li>
 *   <li>사용자 (FIRS/FINA_REG_USER_ID): JPA Auditing @CreatedBy / @LastModifiedBy + SpringSecurityAuditorAware</li>
 *   <li>프로그램ID, IP: @PrePersist/@PreUpdate + {@link AuditContext}</li>
 *   <li>WRK_RMK: 수동 (사용자 입력)</li>
 * </ul>
 *
 * @see <a href="../../../../../../../../../guide/decisions/ADR-005-audit-columns-auto-injection.md">ADR-005</a>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @Column(name = "WRK_RMK", length = 100)
    private String wrkRmk;

    // ===== 최초 등록 =====
    @Column(name = "FIRS_REG_PGM_ID", length = 50, nullable = false, updatable = false)
    private String firsRegPgmId;

    @CreatedDate
    @Column(name = "FIRS_REG_DTS", nullable = false, updatable = false)
    private LocalDateTime firsRegDts;

    @CreatedBy
    @Column(name = "FIRS_REG_USER_ID", length = 20, nullable = false, updatable = false)
    private String firsRegUserId;

    @Column(name = "FIRS_REG_IP", length = 50, nullable = false, updatable = false)
    private String firsRegIp;

    // ===== 최종 수정 =====
    @Column(name = "FINA_REG_PGM_ID", length = 50, nullable = false)
    private String finaRegPgmId;

    @LastModifiedDate
    @Column(name = "FINA_REG_DTS", nullable = false)
    private LocalDateTime finaRegDts;

    @LastModifiedBy
    @Column(name = "FINA_REG_USER_ID", length = 20, nullable = false)
    private String finaRegUserId;

    @Column(name = "FINA_REG_IP", length = 50, nullable = false)
    private String finaRegIp;

    @PrePersist
    void prePersist() {
        var ctx = AuditContext.current();
        this.firsRegPgmId = ctx.pgmId();
        this.firsRegIp    = ctx.ip();
        this.finaRegPgmId = ctx.pgmId();
        this.finaRegIp    = ctx.ip();
    }

    @PreUpdate
    void preUpdate() {
        var ctx = AuditContext.current();
        this.finaRegPgmId = ctx.pgmId();
        this.finaRegIp    = ctx.ip();
    }

    /** WRK_RMK (업무비고) 설정 — 도메인 서비스에서 명시적으로 호출. */
    protected void updateWrkRmk(String wrkRmk) {
        this.wrkRmk = wrkRmk;
    }
}
