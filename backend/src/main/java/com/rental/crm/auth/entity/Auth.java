package com.rental.crm.auth.entity;

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
 * 권한 키 마스터 — `CM_AUTH` (ADR-008).
 *
 * <p>본 사이클에서는 화면 CRUD 없음 (시드만). 화면 신설 시 별도 사이클에서.
 * `MENU_ID` 는 LEAF 메뉴를 가리키며 NULL = 글로벌 권한.
 */
@Entity
@Table(name = "CM_AUTH")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends BaseAuditEntity {

    @Id
    @Column(name = "AUTH_CODE", length = 50, nullable = false)
    private String authCode;

    @Column(name = "AUTH_NAME", length = 100, nullable = false)
    private String authName;

    @Column(name = "MENU_ID")
    private Long menuId;

    @Column(name = "AUTH_TYPE", length = 20, nullable = false)
    private String authType;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Auth(String authCode, String authName, Long menuId,
                 String authType, Integer sortOrder) {
        this.authCode  = authCode;
        this.authName  = authName;
        this.menuId    = menuId;
        this.authType  = authType;
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
        this.useYn     = "Y";
    }
}
