package com.rental.crm.menu.dto;

import org.springframework.lang.Nullable;

public record MenuSearchRequest(
        @Nullable String menuName,
        @Nullable String menuType,
        @Nullable String useYn
) {
    public boolean hasMenuName() { return menuName != null && !menuName.isBlank(); }
    public boolean hasMenuType() { return menuType != null && !menuType.isBlank(); }
    public boolean hasUseYn()    { return useYn    != null && !useYn.isBlank(); }
}
