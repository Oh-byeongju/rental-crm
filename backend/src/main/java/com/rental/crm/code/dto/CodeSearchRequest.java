package com.rental.crm.code.dto;

import org.springframework.lang.Nullable;

public record CodeSearchRequest(
        @Nullable String codeValue,
        @Nullable String codeName,
        @Nullable String useYn
) {
    public boolean hasCodeValue() { return codeValue != null && !codeValue.isBlank(); }
    public boolean hasCodeName()  { return codeName  != null && !codeName.isBlank(); }
    public boolean hasUseYn()     { return useYn     != null && !useYn.isBlank(); }
}
