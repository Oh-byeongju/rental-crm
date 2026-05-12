package com.rental.crm.contract.controller;

import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import com.rental.crm.contract.dto.ContractActionRequest;
import com.rental.crm.contract.dto.ContractCreateRequest;
import com.rental.crm.contract.dto.ContractResponse;
import com.rental.crm.contract.dto.ContractSearchRequest;
import com.rental.crm.contract.dto.ContractUpdateRequest;
import com.rental.crm.contract.service.ContractService;
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
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractRestController {

    private final ContractService contractService;

    @GetMapping
    public ApiResponse<PageResponse<ContractResponse>> search(
            @ModelAttribute ContractSearchRequest search,
            @PageableDefault(size = 20, sort = "contractId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(contractService.search(search, pageable)));
    }

    @GetMapping("/{contractId}")
    public ApiResponse<ContractResponse> detail(@PathVariable Long contractId) {
        return ApiResponse.ok(contractService.findById(contractId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> register(
            @Valid @RequestBody ContractCreateRequest req) {
        var created = contractService.register(req);
        return ResponseEntity
                .created(URI.create("/api/contracts/" + created.contractId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{contractId}")
    public ApiResponse<ContractResponse> update(@PathVariable Long contractId,
                                                @Valid @RequestBody ContractUpdateRequest req) {
        return ApiResponse.ok(contractService.update(contractId, req), "수정되었습니다");
    }

    // ===== 상태 전이 EXECUTE 액션 (CONTRACT_SUSPEND / RESUME / TERMINATE) =====

    @PutMapping("/{contractId}/suspend")
    public ApiResponse<ContractResponse> suspend(@PathVariable Long contractId,
                                                 @Valid @RequestBody ContractActionRequest req) {
        return ApiResponse.ok(contractService.suspend(contractId, req), "일시정지되었습니다");
    }

    @PutMapping("/{contractId}/resume")
    public ApiResponse<ContractResponse> resume(@PathVariable Long contractId) {
        return ApiResponse.ok(contractService.resume(contractId), "재개되었습니다");
    }

    @PutMapping("/{contractId}/terminate")
    public ApiResponse<ContractResponse> terminate(@PathVariable Long contractId,
                                                   @Valid @RequestBody ContractActionRequest req) {
        return ApiResponse.ok(contractService.terminate(contractId, req), "해지되었습니다");
    }
}
