package com.rental.backoffice.common.controller;

import com.rental.backoffice.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 백오피스 공통 라우팅 — 대시보드 / 로그인.
 *
 * <p>실제 도메인 페이지는 각 도메인 PageController 가 처리. 모든 시드 메뉴(CM_MENU)
 * 가 전용 Controller 를 가지므로 미구현 placeholder 는 더 이상 없다.
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final DashboardService dashboardService;

    /** 루트 + 사이드바의 "대시보드" 메뉴 모두 dashboard.html 반환. 위젯은 서버측 렌더(캐시 우선). */
    @GetMapping({"/", "/admin/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "대시보드");
        model.addAttribute("summary", dashboardService.getSummary());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
