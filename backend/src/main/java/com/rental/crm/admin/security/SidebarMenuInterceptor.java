package com.rental.crm.admin.security;

import com.rental.crm.admin.service.SidebarService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 백오피스 페이지 진입 시 사이드바 메뉴 트리 + 현재 URL 자동 주입.
 *
 * <p>REST 요청(ModelAndView=null) 은 스킵. 정적 자원은 WebMvcConfig 에서 제외.
 */
@Component
@RequiredArgsConstructor
public class SidebarMenuInterceptor implements HandlerInterceptor {

    private static final String ANONYMOUS = "anonymousUser";

    private final SidebarService sidebarService;

    @Override
    public void postHandle(HttpServletRequest req,
                           HttpServletResponse res,
                           Object handler,
                           ModelAndView mv) {
        if (mv == null) return; // REST / forwarded — view 없음

        Long userId = currentUserId();
        mv.addObject("sidebarMenus", sidebarService.findVisibleMenuTree(userId));
        mv.addObject("currentUserId", userId);
        mv.addObject("currentPath", req.getRequestURI());
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (ANONYMOUS.equals(principal)) return null;
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
