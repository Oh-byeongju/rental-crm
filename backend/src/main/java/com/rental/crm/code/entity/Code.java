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

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Code(String groupCode, String codeValue, String codeName, Integer sortOrder) {
        this.groupCode = groupCode;
        this.codeValue = codeValue;
        this.codeName  = codeName;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.useYn     = "Y";
    }

    public void changeName(String codeName) {
        this.codeName = codeName;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void changeUseYn(String useYn) {
        this.useYn = useYn;
    }

    public void deactivate() {
        this.useYn = "N";
    }
}
