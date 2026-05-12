package com.rental.crm.code.entity;

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
 * 공통코드 — `CM_CODE`. UNIQUE (GROUP_CODE, CODE_VALUE).
 *
 * <p>확장 필드 (참고 프로젝트 sy_code_dtl 구조 차용):
 * <ul>
 *   <li>DESCRIPTION: 코드 설명 (모호한 코드값 부연)</li>
 *   <li>PROP_VAL1~3: 도메인별 자유 확장 속성 (단축어 / 표시색 / 외부키 등)</li>
 * </ul>
 */
@Entity
@Table(name = "CM_CODE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Code extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_code")
    @SequenceGenerator(name = "seq_cm_code", sequenceName = "SEQ_CM_CODE", allocationSize = 50)
    @Column(name = "CODE_ID")
    private Long codeId;

    @Column(name = "GROUP_CODE", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "CODE_VALUE", length = 50, nullable = false)
    private String codeValue;

    @Column(name = "CODE_NAME", length = 100, nullable = false)
    private String codeName;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "PROP_VAL1", length = 100)
    private String propVal1;

    @Column(name = "PROP_VAL2", length = 100)
    private String propVal2;

    @Column(name = "PROP_VAL3", length = 100)
    private String propVal3;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Code(String groupCode, String codeValue, String codeName, Integer sortOrder,
                 String description, String propVal1, String propVal2, String propVal3) {
        this.groupCode = groupCode;
        this.codeValue = codeValue;
        this.codeName  = codeName;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.description = description;
        this.propVal1 = propVal1;
        this.propVal2 = propVal2;
        this.propVal3 = propVal3;
        this.useYn = "Y";
    }

    // ===== 도메인 행위 =====
    public void changeName(String codeName)              { this.codeName    = codeName; }
    public void changeSortOrder(Integer sortOrder)       { this.sortOrder   = sortOrder == null ? 0 : sortOrder; }
    public void changeDescription(String description)    { this.description = description; }
    public void changePropVal1(String propVal1)          { this.propVal1    = propVal1; }
    public void changePropVal2(String propVal2)          { this.propVal2    = propVal2; }
    public void changePropVal3(String propVal3)          { this.propVal3    = propVal3; }
    public void changeUseYn(String useYn)                { this.useYn       = useYn; }
    public void deactivate()                             { this.useYn       = "N"; }
}
