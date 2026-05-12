package com.rental.crm.overdue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/overdues")
public class OverduePageController {

    @GetMapping
    public String list() {
        return "overdue/list";
    }
}
