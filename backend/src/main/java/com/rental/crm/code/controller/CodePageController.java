package com.rental.crm.code.controller;

import com.rental.crm.code.dto.CodeGroupResponse;
import com.rental.crm.code.service.CodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 공통코드 화면 컨트롤러.
 *  - /admin/codes              → 그룹 목록 (code/list.html)
 *  - /admin/codes/{groupCode}  → 그룹별 코드 목록 (code/code-list.html)
 */
@Controller
@RequestMapping("/admin/codes")
@RequiredArgsConstructor
public class CodePageController {

    private final CodeService codeService;

    @GetMapping
    public String groupList() {
        return "code/list";
    }

    @GetMapping("/{groupCode}")
    public String codeList(@PathVariable String groupCode, Model model) {
        CodeGroupResponse group = codeService.findGroup(groupCode);
        model.addAttribute("group", group);
        return "code/code-list";
    }
}
