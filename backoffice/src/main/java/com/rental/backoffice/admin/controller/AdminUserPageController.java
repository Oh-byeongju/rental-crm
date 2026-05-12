package com.rental.backoffice.admin.controller;

import com.rental.backoffice.admin.dto.AdminUserResponse;
import com.rental.backoffice.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserPageController {

    private final AdminUserService userService;

    @GetMapping
    public String list() {
        return "user/list";
    }

    /** 사용자별 권한 미세 조정 (ADR-009). */
    @GetMapping("/{userId}/auths")
    public String authMatrix(@PathVariable Long userId, Model model) {
        AdminUserResponse user = userService.findById(userId);
        model.addAttribute("user", user);
        return "user/auth-matrix";
    }
}
