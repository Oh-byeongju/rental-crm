package com.rental.backoffice.report.controller;

import com.rental.backoffice.report.dto.OverdueReportRow;
import com.rental.backoffice.report.dto.OverdueReportSearchRequest;
import com.rental.backoffice.report.service.OverdueReportService;
import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 미납 현황 리포트 API (Ch.2). 07 API 명세 §13-2.
 * <ul>
 *   <li>{@code GET /api/reports/overdue}        — 화면 페이징 조회 (JSON)</li>
 *   <li>{@code GET /api/reports/overdue/excel}  — SXSSF 스트리밍 다운로드 (xlsx)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/reports/overdue")
@RequiredArgsConstructor
public class OverdueReportRestController {

    private final OverdueReportService overdueReportService;

    @GetMapping
    public ApiResponse<PageResponse<OverdueReportRow>> search(
            @ModelAttribute OverdueReportSearchRequest search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(overdueReportService.search(search, pageable)));
    }

    @GetMapping("/excel")
    public void excel(@ModelAttribute OverdueReportSearchRequest search,
                      HttpServletResponse response) {
        overdueReportService.exportExcel(search, response);
    }
}
