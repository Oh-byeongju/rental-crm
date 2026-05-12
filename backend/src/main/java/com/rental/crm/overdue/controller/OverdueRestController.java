package com.rental.crm.overdue.controller;

import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import com.rental.crm.overdue.dto.OverdueResolveRequest;
import com.rental.crm.overdue.dto.OverdueResponse;
import com.rental.crm.overdue.dto.OverdueSearchRequest;
import com.rental.crm.overdue.service.OverdueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overdues")
@RequiredArgsConstructor
public class OverdueRestController {

    private final OverdueService overdueService;

    @GetMapping
    public ApiResponse<PageResponse<OverdueResponse>> search(
            @ModelAttribute OverdueSearchRequest search,
            @PageableDefault(size = 20, sort = "overdueDays",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(overdueService.search(search, pageable)));
    }

    @GetMapping("/{overdueId}")
    public ApiResponse<OverdueResponse> detail(@PathVariable Long overdueId) {
        return ApiResponse.ok(overdueService.findById(overdueId));
    }

    @PutMapping("/{overdueId}/resolve")
    public ApiResponse<OverdueResponse> resolve(@PathVariable Long overdueId,
                                                @Valid @RequestBody OverdueResolveRequest req) {
        return ApiResponse.ok(overdueService.resolveManually(overdueId, req), "연체가 해제되었습니다");
    }
}
