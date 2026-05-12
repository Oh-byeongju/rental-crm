package com.rental.domain.menu.entity;

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

/**
 * 메뉴 — `CM_MENU`.
 *
 * <p>2-depth 트리 (PARENT_MENU_ID NULL = 루트). MENU_TYPE GROUP/LEAF.
 * GROUP 은 자식 펼침, LEAF 는 URL 이동.
 *
 * <p>회피 정책 (학습 단순화):
 * <ul>
 *   <li>등록 후 MENU_TYPE / PARENT_MENU_ID 변경 불가 — 트리 정합성 우선</li>
 *   <li>변경 필요 시 삭제 후 재등록</li>
 * </ul>
 */
@Entity
@Table(name = "CM_MENU")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_menu")
    @SequenceGenerator(name = "seq_cm_menu", sequenceName = "SEQ_CM_MENU", allocationSize = 50)
    @Column(name = "MENU_ID")
    private Long menuId;

    @Column(name = "PARENT_MENU_ID")
    private Long parentMenuId;

    @Column(name = "MENU_DEPTH", nullable = false)
    private Integer menuDepth;

    @Column(name = "MENU_NAME", length = 100, nullable = false)
    private String menuName;

    @Column(name = "MENU_TYPE", length = 20, nullable = false)
    private String menuType;

    @Column(name = "MENU_URL", length = 200)
    private String menuUrl;

    @Column(name = "ICON_CLASS", length = 50)
    private String iconClass;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Menu(Long parentMenuId,
                 Integer menuDepth,
                 String menuName,
                 String menuType,
                 String menuUrl,
                 String iconClass,
                 Integer sortOrder) {
        this.parentMenuId = parentMenuId;
        this.menuDepth    = menuDepth;
        this.menuName     = menuName;
        this.menuType     = menuType;
        this.menuUrl      = menuUrl;
        this.iconClass    = iconClass;
        this.sortOrder    = sortOrder == null ? 0 : sortOrder;
        this.useYn        = "Y";
    }

    public void changeName(String menuName) {
        this.menuName = menuName;
    }

    public void changeUrl(String menuUrl) {
        this.menuUrl = menuUrl;
    }

    public void changeIcon(String iconClass) {
        this.iconClass = iconClass;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder == null ? 0 : sortOrder;
    }

    public void changeUseYn(String useYn) {
        this.useYn = useYn;
    }

    public boolean isGroup() {
        return "GROUP".equals(this.menuType);
    }

    public boolean isLeaf() {
        return "LEAF".equals(this.menuType);
    }
}
