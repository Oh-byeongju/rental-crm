package com.rental.backoffice.product.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/products")
public class ProductPageController {

    @GetMapping
    public String list() {
        return "product/list";
    }
}
