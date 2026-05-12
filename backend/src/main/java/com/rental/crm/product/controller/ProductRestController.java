package com.rental.crm.product.controller;

import com.rental.crm.common.response.ApiResponse;
import com.rental.crm.common.response.PageResponse;
import com.rental.crm.product.dto.ProductCreateRequest;
import com.rental.crm.product.dto.ProductResponse;
import com.rental.crm.product.dto.ProductSearchRequest;
import com.rental.crm.product.dto.ProductUpdateRequest;
import com.rental.crm.product.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> search(
            @ModelAttribute ProductSearchRequest search,
            @PageableDefault(size = 20, sort = "productId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(productService.search(search, pageable)));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long productId) {
        return ApiResponse.ok(productService.findById(productId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> register(
            @Valid @RequestBody ProductCreateRequest req) {
        var created = productService.register(req);
        return ResponseEntity
                .created(URI.create("/api/products/" + created.productId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable Long productId,
                                               @Valid @RequestBody ProductUpdateRequest req) {
        return ApiResponse.ok(productService.update(productId, req), "수정되었습니다");
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deactivate(@PathVariable Long productId) {
        productService.deactivate(productId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }
}
