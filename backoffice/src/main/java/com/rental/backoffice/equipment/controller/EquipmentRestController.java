package com.rental.backoffice.equipment.controller;

import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import com.rental.backoffice.equipment.dto.EquipmentCreateRequest;
import com.rental.backoffice.equipment.dto.EquipmentResponse;
import com.rental.backoffice.equipment.dto.EquipmentSearchRequest;
import com.rental.backoffice.equipment.dto.EquipmentUpdateRequest;
import com.rental.backoffice.equipment.service.EquipmentService;
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
@RequestMapping("/api/equipments")
@RequiredArgsConstructor
public class EquipmentRestController {

    private final EquipmentService equipmentService;

    @GetMapping
    public ApiResponse<PageResponse<EquipmentResponse>> search(
            @ModelAttribute EquipmentSearchRequest search,
            @PageableDefault(size = 20, sort = "equipmentId") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(equipmentService.search(search, pageable)));
    }

    @GetMapping("/{equipmentId}")
    public ApiResponse<EquipmentResponse> detail(@PathVariable Long equipmentId) {
        return ApiResponse.ok(equipmentService.findById(equipmentId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EquipmentResponse>> register(
            @Valid @RequestBody EquipmentCreateRequest req) {
        var created = equipmentService.register(req);
        return ResponseEntity
                .created(URI.create("/api/equipments/" + created.equipmentId()))
                .body(ApiResponse.ok(created, "등록되었습니다"));
    }

    @PutMapping("/{equipmentId}")
    public ApiResponse<EquipmentResponse> update(@PathVariable Long equipmentId,
                                                 @Valid @RequestBody EquipmentUpdateRequest req) {
        return ApiResponse.ok(equipmentService.update(equipmentId, req), "수정되었습니다");
    }

    @DeleteMapping("/{equipmentId}")
    public ApiResponse<Void> deactivate(@PathVariable Long equipmentId) {
        equipmentService.deactivate(equipmentId);
        return ApiResponse.ok(null, "비활성화되었습니다");
    }
}
