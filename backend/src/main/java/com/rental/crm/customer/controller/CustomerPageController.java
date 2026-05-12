package com.rental.crm.customer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 고객 관리 화면 라우팅 — Thymeleaf 템플릿 반환.
 * 실제 데이터는 CustomerRestController 의 JSON API 를 화면 JS 가 호출.
 */
@Controller
@RequestMapping("/admin/customers")
public class CustomerPageController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "고객 관리");
        return "customer/list";
    }
}
