package com.rental.backoffice.customer.dto;

/**
 * 고객 목록 검색 조건. 모든 필드 optional. null/빈문자열 = 조건 미적용.
 */
public record CustomerSearchRequest(
        String name,
        String phone,
        String email,
        String useYn
) {
    public boolean hasName()  { return name != null && !name.isBlank(); }
    public boolean hasPhone() { return phone != null && !phone.isBlank(); }
    public boolean hasEmail() { return email != null && !email.isBlank(); }
    public boolean hasUseYn() { return useYn != null && !useYn.isBlank(); }
}
