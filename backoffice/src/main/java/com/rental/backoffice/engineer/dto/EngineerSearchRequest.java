package com.rental.backoffice.engineer.dto;

import org.springframework.lang.Nullable;

public record EngineerSearchRequest(
        @Nullable String engineerCode,
        @Nullable String engineerName,
        @Nullable String engineerType,
        @Nullable String area,
        @Nullable String useYn
) {
    public boolean hasEngineerCode() { return engineerCode != null && !engineerCode.isBlank(); }
    public boolean hasEngineerName() { return engineerName != null && !engineerName.isBlank(); }
    public boolean hasEngineerType() { return engineerType != null && !engineerType.isBlank(); }
    public boolean hasArea()         { return area         != null && !area.isBlank(); }
    public boolean hasUseYn()        { return useYn        != null && !useYn.isBlank(); }
}
