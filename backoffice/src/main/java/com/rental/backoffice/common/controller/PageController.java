package com.rental.backoffice.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 백오피스 공통 라우팅 — 대시보드/로그인 + 미구현 도메인 placeholder.
 *
 * <p>실제 도메인 페이지 (Code/Menu/Role/User/Customer) 는 각 도메인 PageController 가 처리.
 * 본 클래스는 시드 메뉴 (CM_MENU) 의 URL 과 1:1 매칭 — 시드 URL 변경 시 본 표 동기 필수.
 */
@Controller
public class PageController {

    /** 시드 메뉴 URL → 표시명. 도메인 Controller 가 추가되면 본 맵에서 제거. */
    private static final Map<String, String> PLACEHOLDER_MENUS = new LinkedHashMap<>() {{
        // 전용 PageController 가 아직 없는 미구현 경로만 placeholder 로 남긴다.
        // 도메인 Controller 가 추가되면 본 맵 + 아래 @GetMapping 에서 제거.
        put("/admin/reports/overdue", "미납 현황 엑셀");
    }};

    /** 루트 + 사이드바의 "대시보드" 메뉴 모두 dashboard.html 반환. */
    @GetMapping({"/", "/admin/dashboard"})
    public String dashboard(Model model) {
        // TODO: GET /api/dashboard/summary 호출로 summary 채우기 (현재 placeholder)
        model.addAttribute("pageTitle", "대시보드");
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 미구현 도메인 일괄 placeholder — 시드 메뉴 (`/admin/*`) 와 1:1 매칭.
     * 도메인 Controller 가 구현되면 본 클래스의 PLACEHOLDER_MENUS + GetMapping 에서 제거.
     */
    @GetMapping("/admin/reports/overdue")
    public String placeholder(HttpServletRequest req, Model model) {
        var path = req.getRequestURI();
        model.addAttribute("requestPath", path);
        model.addAttribute("menuName", PLACEHOLDER_MENUS.getOrDefault(path, "메뉴"));
        model.addAttribute("pageTitle", PLACEHOLDER_MENUS.getOrDefault(path, "준비 중"));
        return "placeholder";
    }
}
