package com.rental.crm.code.entity;

import com.rental.crm.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통코드 그룹 — `CM_CODE_GROUP`.
 * 04 §1-1 / 06 ERD 기준. PK 는 사람이 읽는 GROUP_CODE (예: BILLING_STATUS).
 */
@Entity
@Table(name = "CM_CODE_GROUP")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends BaseAuditEntity {

    @Id
    @Column(name = "GROUP_CODE", length = 50, nullable = false)
    private String groupCode;

    @Column(name = "GROUP_NAME", length = 100, nullable = false)
    private String groupName;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private CodeGroup(String groupCode, String groupName, String description) {
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
        this.useYn = "Y";
    }

    public void changeName(String groupName) {
        this.groupName = groupName;
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void changeUseYn(String useYn) {
        this.useYn = useYn;
    }

    public void deactivate() {
        this.useYn = "N";
    }
}
