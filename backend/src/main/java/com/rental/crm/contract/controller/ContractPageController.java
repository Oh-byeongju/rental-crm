package com.rental.crm.contract.controller;

import com.rental.crm.contract.dto.ContractResponse;
import com.rental.crm.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/contracts")
@RequiredArgsConstructor
public class ContractPageController {

    private final ContractService contractService;

    @GetMapping
    public String list() {
        return "contract/list";
    }

    /** 별도 상세 페이지 — D10 B 채택 (상태 전이 액션 분리 표시). */
    @GetMapping("/{contractId}")
    public String detail(@PathVariable Long contractId, Model model) {
        ContractResponse contract = contractService.findById(contractId);
        model.addAttribute("contract", contract);
        return "contract/detail";
    }
}
