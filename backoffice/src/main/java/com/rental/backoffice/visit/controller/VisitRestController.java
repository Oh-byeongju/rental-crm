package com.rental.backoffice.visit.controller;

import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import com.rental.backoffice.visit.dto.VisitCancelRequest;
import com.rental.backoffice.visit.dto.VisitCreateRequest;
import com.rental.backoffice.visit.dto.VisitResponse;
import com.rental.backoffice.visit.dto.VisitSearchRequest;
import com.rental.backoffice.visit.dto.VisitUpdateRequest;
import com.rental.backoffice.visit.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitRestController {

    private final VisitService visitService;

    @GetMapping
    public ApiResponse<PageResponse<VisitResponse>> search(
            @ModelAttribute VisitSearchRequest search,
            @PageableDefault(size = 20, sort = "visitId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(visitService.search(search, pageable)));
    }

    @GetMapping("/{visitId}")
    public ApiResponse<VisitResponse> detail(@PathVariable Long visitId) {
        return ApiResponse.ok(visitService.findById(visitId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VisitResponse>> register(
            @Valid @RequestBody VisitCreateRequest req) {
        var created = visitService.register(req);
        return ResponseEntity
                .created(URI.create("/api/visits/" + created.visitId()))
                .body(ApiResponse.ok(created, "배정되었습니다"));
    }

    @PutMapping("/{visitId}")
    public ApiResponse<VisitResponse> update(@PathVariable Long visitId,
                                             @Valid @RequestBody VisitUpdateRequest req) {
        return ApiResponse.ok(visitService.update(visitId, req), "수정되었습니다");
    }

    // ===== 상태 전이 EXECUTE 액션 =====

    @PutMapping("/{visitId}/complete")
    public ApiResponse<VisitResponse> complete(@PathVariable Long visitId) {
        return ApiResponse.ok(visitService.complete(visitId), "완료 처리되었습니다");
    }

    @PutMapping("/{visitId}/cancel")
    public ApiResponse<VisitResponse> cancel(@PathVariable Long visitId,
                                             @Valid @RequestBody VisitCancelRequest req) {
        return ApiResponse.ok(visitService.cancel(visitId, req), "취소되었습니다");
    }
}
