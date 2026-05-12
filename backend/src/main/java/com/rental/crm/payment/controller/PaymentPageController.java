package com.rental.crm.payment.controller;

import com.rental.crm.payment.dto.PaymentResponse;
import com.rental.crm.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class PaymentPageController {

    private final PaymentService paymentService;

    @GetMapping
    public String list() {
        return "payment/list";
    }

    /** 상세 — 취소/환불 액션 분리 표시 (D10 B 패턴, 계약 도메인과 일치). */
    @GetMapping("/{paymentId}")
    public String detail(@PathVariable Long paymentId, Model model) {
        PaymentResponse payment = paymentService.findById(paymentId);
        model.addAttribute("payment", payment);
        return "payment/detail";
    }
}
