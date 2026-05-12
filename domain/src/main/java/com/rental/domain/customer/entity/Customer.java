package com.rental.domain.customer.entity;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 고객 엔티티 — `CT_CUSTOMER`.
 * ADR-002 의 컬럼 정의 기준.
 *
 * <p>상태 변경은 명시적 도메인 메서드만 허용 (Setter 미노출).
 */
@Entity
@Table(name = "CT_CUSTOMER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_customer")
    @SequenceGenerator(name = "seq_ct_customer", sequenceName = "SEQ_CT_CUSTOMER",
                       allocationSize = 50)
    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "CUSTOMER_NO", length = 20, nullable = false, unique = true)
    private String customerNo;

    @Column(name = "CUSTOMER_NAME", length = 100, nullable = false)
    private String customerName;

    @Column(name = "EMAIL", length = 254, nullable = false, unique = true)
    private String email;

    @Column(name = "PASSWORD", length = 255, nullable = false)
    private String password;

    @Column(name = "PHONE", length = 20, nullable = false)
    private String phone;

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "ADDRESS_ZIP", length = 10)
    private String addressZip;

    @Column(name = "ADDRESS", length = 500, nullable = false)
    private String address;

    @Column(name = "TERMS_AGREE_YN", length = 1, nullable = false)
    private String termsAgreeYn;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Column(name = "LOGIN_FAIL_CNT", nullable = false)
    private Integer loginFailCnt;

    @Column(name = "LOCKED_AT")
    private LocalDateTime lockedAt;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Builder
    private Customer(String customerNo,
                     String customerName,
                     String email,
                     String password,
                     String phone,
                     LocalDate birthDate,
                     String addressZip,
                     String address,
                     String termsAgreeYn) {
        this.customerNo     = customerNo;
        this.customerName   = customerName;
        this.email          = email;
        this.password       = password;
        this.phone          = phone;
        this.birthDate      = birthDate;
        this.addressZip     = addressZip;
        this.address        = address;
        this.termsAgreeYn   = termsAgreeYn;
        this.useYn          = "Y";
        this.loginFailCnt   = 0;
    }

    // ===== 도메인 행위 =====

    public void changeAddress(String addressZip, String address) {
        this.addressZip = addressZip;
        this.address    = address;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.loginFailCnt = 0;
        this.lockedAt = null;
    }

    public void recordFailedLogin() {
        this.loginFailCnt = (this.loginFailCnt == null ? 0 : this.loginFailCnt) + 1;
        if (this.loginFailCnt >= 5) {
            this.lockedAt = LocalDateTime.now();
        }
    }

    /** 잠금 해제 정책: LOCKED_AT + 30분 < SYSDATE 면 자동 해제 (ADR-001 §2-5). */
    public boolean isLocked() {
        return this.lockedAt != null
                && this.lockedAt.plusMinutes(30).isAfter(LocalDateTime.now());
    }

    public void deactivate() {
        this.useYn = "N";
    }

    /**
     * 사용여부 변경 — 모달 select 로 활성/비활성 일괄 전환.
     * Y/N 외 값은 호출 측에서 검증되었다고 가정.
     */
    public void changeUseYn(String useYn) {
        this.useYn = useYn;
    }
}
