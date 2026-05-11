package com.rental.crm.admin.entity;

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

import java.time.LocalDateTime;

/**
 * 백오피스 관리자 — `CM_USER`.
 *
 * <p>잠금 정책 (06 ERD CM_USER §정책 메모, ADR-001):
 * <ul>
 *   <li>5회 연속 로그인 실패 시 `LOCKED_AT = SYSDATE` 기록</li>
 *   <li>로그인 시도 시 `LOCKED_AT + 30분 < SYSDATE` 이면 자동 해제</li>
 *   <li>관리자 수동 해제 가능 (`LOCKED_AT = NULL`, `LOGIN_FAIL_CNT = 0`)</li>
 * </ul>
 */
@Entity
@Table(name = "CM_USER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUser extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_user")
    @SequenceGenerator(name = "seq_cm_user", sequenceName = "SEQ_CM_USER", allocationSize = 50)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "EMAIL", length = 254, nullable = false, unique = true)
    private String email;

    @Column(name = "PASSWORD", length = 255, nullable = false)
    private String password;

    @Column(name = "USER_NAME", length = 100, nullable = false)
    private String userName;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "ROLE_ID")
    private Long roleId;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Column(name = "LOGIN_FAIL_CNT", nullable = false)
    private Integer loginFailCnt;

    @Column(name = "LOCKED_AT")
    private LocalDateTime lockedAt;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Builder
    private AdminUser(String email, String password, String userName,
                      String phone, Long roleId) {
        this.email        = email;
        this.password     = password;
        this.userName     = userName;
        this.phone        = phone;
        this.roleId       = roleId;
        this.useYn        = "Y";
        this.loginFailCnt = 0;
    }

    // ===== 도메인 행위 =====

    public void changeName(String userName)   { this.userName = userName; }
    public void changePhone(String phone)     { this.phone    = phone; }
    public void changeRole(Long roleId)       { this.roleId   = roleId; }
    public void changePassword(String hash)   { this.password = hash; }
    public void changeUseYn(String useYn)     { this.useYn    = useYn; }
    public void deactivate()                  { this.useYn    = "N"; }

    /** 관리자 화면의 [잠금 해제] — LOCKED_AT/실패 카운트 초기화. */
    public void unlock() {
        this.lockedAt     = null;
        this.loginFailCnt = 0;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt  = LocalDateTime.now();
        this.loginFailCnt = 0;
        this.lockedAt     = null;
    }

    public void recordFailedLogin() {
        this.loginFailCnt = (this.loginFailCnt == null ? 0 : this.loginFailCnt) + 1;
        if (this.loginFailCnt >= 5) {
            this.lockedAt = LocalDateTime.now();
        }
    }

    /** 30분 미경과면 잠금 유지. (ADR-001 — 30분 후 자동 해제). */
    public boolean isLocked() {
        return this.lockedAt != null
                && this.lockedAt.plusMinutes(30).isAfter(LocalDateTime.now());
    }
}
