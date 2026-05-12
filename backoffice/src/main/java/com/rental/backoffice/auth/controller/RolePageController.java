package com.rental.backoffice.auth.controller;

import com.rental.backoffice.auth.dto.RoleResponse;
import com.rental.backoffice.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RolePageController {

    private final RoleService roleService;

    /** 역할 목록. */
    @GetMapping
    public String list() {
        return "role/list";
    }

    /** 역할별 AUTH 매트릭스. */
    @GetMapping("/{roleId}/auths")
    public String authMatrix(@PathVariable Long roleId, Model model) {
        RoleResponse role = roleService.findById(roleId);
        model.addAttribute("role", role);
        return "role/auth-matrix";
    }
}
