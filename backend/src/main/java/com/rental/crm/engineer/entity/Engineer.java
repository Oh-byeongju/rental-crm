package com.rental.crm.engineer.entity;

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

import java.util.Set;

/**
 * 설치/점검 기사 — `CT_ENGINEER`.
 *
 * <p>정책:
 * <ul>
 *   <li>ENGINEER_CODE 는 UNIQUE 비즈니스 키 — 변경 가능 (PRODUCT_CODE 패턴)</li>
 *   <li>ENGINEER_TYPE: INTERNAL (내부 직원) / EXTERNAL (외주) — enum 직접 저장 (CM_CODE 미사용, ERD §메모 일치)</li>
 *   <li>AREA: 자유 텍스트 (추후 코드화 검토). 지역별 기사 조회 — `IDX_CT_ENGINEER_AREA` 활용</li>
 *   <li>비활성화: USE_YN='N' (소프트 삭제)</li>
 * </ul>
 */
@Entity
@Table(name = "CT_ENGINEER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Engineer extends BaseAuditEntity {

    public static final String TYPE_INTERNAL = "INTERNAL";
    public static final String TYPE_EXTERNAL = "EXTERNAL";
    public static final Set<String> VALID_TYPES = Set.of(TYPE_INTERNAL, TYPE_EXTERNAL);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_engineer")
    @SequenceGenerator(name = "seq_ct_engineer", sequenceName = "SEQ_CT_ENGINEER", allocationSize = 50)
    @Column(name = "ENGINEER_ID")
    private Long engineerId;

    @Column(name = "ENGINEER_CODE", length = 20, nullable = false, unique = true)
    private String engineerCode;

    @Column(name = "ENGINEER_NAME", length = 100, nullable = false)
    private String engineerName;

    @Column(name = "ENGINEER_TYPE", length = 20, nullable = false)
    private String engineerType;

    @Column(name = "PHONE", length = 20, nullable = false)
    private String phone;

    @Column(name = "EMAIL", length = 254)
    private String email;

    @Column(name = "AREA", length = 100)
    private String area;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Engineer(String engineerCode, String engineerName, String engineerType,
                     String phone, String email, String area) {
        this.engineerCode = engineerCode;
        this.engineerName = engineerName;
        this.engineerType = engineerType;
        this.phone        = phone;
        this.email        = email;
        this.area         = area;
        this.useYn        = "Y";
    }

    // ===== 도메인 행위 =====
    public void changeEngineerCode(String engineerCode) { this.engineerCode = engineerCode; }
    public void changeEngineerName(String engineerName) { this.engineerName = engineerName; }
    public void changeEngineerType(String engineerType) { this.engineerType = engineerType; }
    public void changePhone(String phone)               { this.phone        = phone; }
    public void changeEmail(String email)               { this.email        = email; }
    public void changeArea(String area)                 { this.area         = area; }
    public void changeUseYn(String useYn)               { this.useYn        = useYn; }
    public void deactivate()                            { this.useYn        = "N"; }
}
