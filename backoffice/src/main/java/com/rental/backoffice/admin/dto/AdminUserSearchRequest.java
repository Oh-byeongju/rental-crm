package com.rental.backoffice.admin.dto;

import org.springframework.lang.Nullable;

public record AdminUserSearchRequest(
        @Nullable String email,
        @Nullable String userName,
        @Nullable String useYn
) {
    public boolean hasEmail()    { return email    != null && !email.isBlank(); }
    public boolean hasUserName() { return userName != null && !userName.isBlank(); }
    public boolean hasUseYn()    { return useYn    != null && !useYn.isBlank(); }
}
