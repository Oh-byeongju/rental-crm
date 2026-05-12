package com.rental.backoffice.billing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/batches")
public class BatchLogPageController {

    @GetMapping
    public String list() {
        return "batch-log/list";
    }
}
