package com.rental.backoffice.engineer.controller;

import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import com.rental.backoffice.engineer.dto.EngineerCreateRequest;
import com.rental.backoffice.engineer.dto.EngineerResponse;
import com.rental.backoffice.engineer.dto.EngineerSearchRequest;
import com.rental.backoffice.engineer.dto.EngineerUpdateRequest;
import com.rental.backoffice.engineer.service.EngineerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/engineers")
@RequiredArgsConstructor
public class EngineerRestController {

    private final EngineerService engineerService;

    @GetMapping
    public ApiResponse<PageResponse<EngineerResponse>> search(
            @ModelAttribute EngineerSearchRequest search,
            @PageableDefault(size = 20, sort = "engineerId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(engineerService.search(search, pageable)));
    }

    @GetMapping("/{engineerId}")
    public ApiResponse<EngineerResponse> detail(@PathVariable Long engineerId) {
        return ApiResponse.ok(engineerService.findById(engineerId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EngineerResponse>> register(
            @Valid @RequestBody EngineerCreateRequest req) {
        var created = engineerService.register(req);
        return ResponseEntity
                .created(URI.create("/api/engineers/" + created.engineerId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{engineerId}")
    public ApiResponse<EngineerResponse> update(@PathVariable Long engineerId,
                                                @Valid @RequestBody EngineerUpdateRequest req) {
        return ApiResponse.ok(engineerService.update(engineerId, req), "수정되었습니다");
    }

    @DeleteMapping("/{engineerId}")
    public ApiResponse<Void> deactivate(@PathVariable Long engineerId) {
        engineerService.deactivate(engineerId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }
}
