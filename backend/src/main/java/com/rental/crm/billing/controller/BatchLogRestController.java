package com.rental.crm.billing.controller;

import com.rental.crm.billing.dto.BatchLogResponse;
import com.rental.crm.billing.dto.BatchLogSearchRequest;
import com.rental.crm.billing.service.BatchLogService;
import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchLogRestController {

    private final BatchLogService batchLogService;

    @GetMapping
    public ApiResponse<PageResponse<BatchLogResponse>> search(
            @ModelAttribute BatchLogSearchRequest search,
            @PageableDefault(size = 20, sort = "batchLogId", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(batchLogService.search(search, pageable)));
    }

    @GetMapping("/{batchLogId}")
    public ApiResponse<BatchLogResponse> detail(@PathVariable Long batchLogId) {
        return ApiResponse.ok(batchLogService.findById(batchLogId));
    }
}
