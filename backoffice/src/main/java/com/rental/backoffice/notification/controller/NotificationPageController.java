package com.rental.backoffice.notification.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/notifications")
public class NotificationPageController {

    @GetMapping
    public String list() {
        return "notification/list";
    }
}
