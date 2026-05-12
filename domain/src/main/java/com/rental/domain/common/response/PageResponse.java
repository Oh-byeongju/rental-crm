package com.rental.domain.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 — 07 API 명세서 §0-3.
 * Spring Data {@link Page} 를 외부 노출용 DTO 로 변환.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
