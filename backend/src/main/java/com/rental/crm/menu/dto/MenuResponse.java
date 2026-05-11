package com.rental.crm.menu.dto;

import com.rental.crm.menu.entity.Menu;

import java.time.LocalDateTime;

public record MenuResponse(
        Long menuId,
        Long parentMenuId,
        Integer menuDepth,
        String menuName,
        String menuType,
        String menuUrl,
        String iconClass,
        Integer sortOrder,
        String useYn,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
    public static MenuResponse from(Menu m) {
        return new MenuResponse(
                m.getMenuId(),
                m.getParentMenuId(),
                m.getMenuDepth(),
                m.getMenuName(),
                m.getMenuType(),
                m.getMenuUrl(),
                m.getIconClass(),
                m.getSortOrder(),
                m.getUseYn(),
                m.getFirsRegDts(),
                m.getFirsRegUserId(),
                m.getFinaRegDts(),
                m.getFinaRegUserId()
        );
    }
}
