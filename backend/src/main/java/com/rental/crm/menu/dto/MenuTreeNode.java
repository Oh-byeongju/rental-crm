package com.rental.crm.menu.dto;

import com.rental.crm.menu.entity.Menu;

import java.util.ArrayList;
import java.util.List;

/**
 * 메뉴 트리 응답 — 사이드바 / 권한 매트릭스 화면 공통 사용.
 * children 은 빈 리스트일 수 있음 (LEAF / 자식 없는 GROUP).
 */
public record MenuTreeNode(
        Long menuId,
        Long parentMenuId,
        Integer menuDepth,
        String menuName,
        String menuType,
        String menuUrl,
        String iconClass,
        Integer sortOrder,
        String useYn,
        List<MenuTreeNode> children
) {
    public static MenuTreeNode of(Menu m) {
        return new MenuTreeNode(
                m.getMenuId(),
                m.getParentMenuId(),
                m.getMenuDepth(),
                m.getMenuName(),
                m.getMenuType(),
                m.getMenuUrl(),
                m.getIconClass(),
                m.getSortOrder(),
                m.getUseYn(),
                new ArrayList<>()
        );
    }
}
