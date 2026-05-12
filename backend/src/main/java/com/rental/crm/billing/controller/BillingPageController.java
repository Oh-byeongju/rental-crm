package com.rental.crm.billing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/billings")
public class BillingPageController {

    @GetMapping
    public String list() {
        return "billing/list";
    }
}
