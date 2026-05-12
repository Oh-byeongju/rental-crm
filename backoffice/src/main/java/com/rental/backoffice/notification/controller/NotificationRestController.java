package com.rental.backoffice.notification.controller;

import com.rental.domain.common.response.ApiResponse;
import com.rental.domain.common.response.PageResponse;
import com.rental.backoffice.notification.dto.NotificationResponse;
import com.rental.backoffice.notification.dto.NotificationSearchRequest;
import com.rental.backoffice.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 알림 REST API — 본인 알림 (broadcast 포함) 조회 + 읽음 처리.
 * 현재 인증: X-User-Id 헤더 시뮬레이션 (ADR-010 §2-8 — JWT 도입 시 SecurityContext 로 전환).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> search(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @ModelAttribute NotificationSearchRequest search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(notificationService.findByUserId(userId, search, pageable)));
    }

    /** 헤더 뱃지용 — 미읽음 카운트만. */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PutMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(notificationService.markRead(notificationId, userId), "읽음 처리되었습니다");
    }

    @PutMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        int updated = notificationService.markAllRead(userId);
        return ApiResponse.ok(Map.of("updated", updated), "전체 읽음 처리되었습니다");
    }
}
