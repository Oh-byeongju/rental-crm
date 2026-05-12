package com.rental.backoffice.engineer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/engineers")
public class EngineerPageController {

    @GetMapping
    public String list() {
        return "engineer/list";
    }
}
