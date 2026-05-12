package com.rental.crm.visit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/visits")
public class VisitPageController {

    @GetMapping
    public String list() {
        return "visit/list";
    }
}
