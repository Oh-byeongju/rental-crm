package com.rental.backoffice.report.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 미납 현황 리포트 화면 (Ch.2). 시드 메뉴 {@code /admin/reports/overdue} 1:1 매칭.
 */
@Controller
@RequestMapping("/admin/reports/overdue")
public class OverdueReportPageController {

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "미납 현황");
        return "report/overdue";
    }
}
