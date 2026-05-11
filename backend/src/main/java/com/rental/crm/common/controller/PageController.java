package com.rental.crm.common.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 백오피스 페이지 라우팅. Thymeleaf 템플릿 반환.
 * 도메인별 페이지 Controller 는 도메인 패키지의 별도 Controller 에서 구현 예정.
 * 본 클래스는 공통/대시보드/로그인 + 미구현 메뉴 placeholder 라우팅.
 */
@Controller
public class PageController {

    /**
     * 사이드바 메뉴 → 표시명 매핑 (placeholder 페이지에서 사용).
     * 도메인별 Controller 가 구현되면 본 맵에서 제거.
     */
    private static final Map<String, String> PLACEHOLDER_MENUS = new LinkedHashMap<>() {{
        put("/sys/codes",          "공통코드 관리");
        put("/sys/roles",          "권한 관리");
        put("/sys/menus",          "메뉴 관리");
        put("/sys/users",          "관리자 계정");
        // "/customers" 는 CustomerPageController 가 처리 (Phase 1B 구현 완료)
        put("/equipments",         "장비 관리");
        put("/products",           "상품 관리");
        put("/contracts",          "계약 관리");
        put("/engineers",          "기사 목록");
        put("/visits",             "방문 이력");
        put("/billings",           "청구 목록");
        put("/billings/batch",     "월 청구 생성 (배치)");
        put("/payments",           "수납 관리");
        put("/overdues",           "연체 관리");
        put("/notifications",      "알림 내역");
        put("/reports/billing",    "청구 현황 엑셀");
        put("/reports/overdue",    "미납 현황 엑셀");
        put("/reports/payment/monthly", "월별 수납 통계");
    }};

    @GetMapping("/")
    public String index(Model model) {
        // TODO: GET /api/dashboard/summary 호출로 summary 채우기 (현재는 placeholder)
        model.addAttribute("pageTitle", "대시보드");
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 미구현 메뉴 일괄 placeholder 라우팅.
     * 사이드바의 모든 메뉴를 본 맵에 등록 → 도메인 Controller 가 구현되면 본 맵에서 제거.
     */
    @GetMapping({
            "/sys/codes",
            "/sys/roles",
            "/sys/menus",
            "/sys/users",
            "/equipments",
            "/products",
            "/contracts",
            "/engineers",
            "/visits",
            "/billings",
            "/billings/batch",
            "/payments",
            "/overdues",
            "/notifications",
            "/reports/billing",
            "/reports/overdue",
            "/reports/payment/monthly"
    })
    public String placeholder(HttpServletRequest req, Model model) {
        var path = req.getRequestURI();
        model.addAttribute("requestPath", path);
        model.addAttribute("menuName", PLACEHOLDER_MENUS.getOrDefault(path, "메뉴"));
        model.addAttribute("pageTitle", PLACEHOLDER_MENUS.getOrDefault(path, "준비 중"));
        return "placeholder";
    }
}
