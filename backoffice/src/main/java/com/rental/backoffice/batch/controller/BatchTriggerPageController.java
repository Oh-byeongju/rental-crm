package com.rental.backoffice.batch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 배치 트리거 화면 (ADR-014 Step 5).
 * <p>현재는 더미 시나리오 2종 (DUMMY_SUCCESS / DUMMY_FAIL) 만 노출.
 * Step 7 부터 실 시나리오로 교체.
 */
@Controller
@RequestMapping("/admin/batch-trigger")
public class BatchTriggerPageController {

    @GetMapping
    public String index() {
        return "batch-trigger/list";
    }
}
