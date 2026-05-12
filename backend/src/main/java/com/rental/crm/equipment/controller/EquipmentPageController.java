package com.rental.crm.equipment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/equipments")
public class EquipmentPageController {

    @GetMapping
    public String list() {
        return "equipment/list";
    }
}
