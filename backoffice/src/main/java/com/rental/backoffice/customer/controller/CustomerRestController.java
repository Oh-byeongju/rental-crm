package com.rental.backoffice.customer.controller;

import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import com.rental.backoffice.customer.dto.CustomerCreateRequest;
import com.rental.backoffice.customer.dto.CustomerResponse;
import com.rental.backoffice.customer.dto.CustomerSearchRequest;
import com.rental.backoffice.customer.dto.CustomerUpdateRequest;
import com.rental.backoffice.customer.service.CustomerService;
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

/**
 * 고객 REST API — 07 API 명세서 §5.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerRestController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResponse<CustomerResponse>> search(
            @ModelAttribute CustomerSearchRequest search,
            @PageableDefault(size = 20, sort = "customerId") Pageable pageable) {
        var page = customerService.search(search, pageable);
        return ApiResponse.ok(PageResponse.from(page));
    }

    @GetMapping("/{customerId}")
    public ApiResponse<CustomerResponse> detail(@PathVariable Long customerId) {
        return ApiResponse.ok(customerService.findById(customerId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> register(
            @Valid @RequestBody CustomerCreateRequest req) {
        var created = customerService.register(req);
        return ResponseEntity
                .created(URI.create("/api/customers/" + created.customerId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{customerId}")
    public ApiResponse<CustomerResponse> update(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerUpdateRequest req) {
        return ApiResponse.ok(customerService.update(customerId, req), "수정되었습니다");
    }

    @DeleteMapping("/{customerId}")
    public ApiResponse<Void> deactivate(@PathVariable Long customerId) {
        customerService.deactivate(customerId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }
}
