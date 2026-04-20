package com.fivefy.domain.notification.controller;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.dto.response.PageResponse;
import com.fivefy.domain.notification.dto.response.NotificationGetResponse;
import com.fivefy.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal Long userId) {
        return notificationService.subscribe(userId);
    }

    @GetMapping("/notifications")
    public ResponseEntity<BaseResponse<PageResponse<NotificationGetResponse>>> getAllNotification (
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<NotificationGetResponse> response = notificationService.getNotifications(userId, pageable);

        return ResponseEntity.status(HttpStatus.OK)
                        .body(BaseResponse.success(
                                HttpStatus.OK, "알림 목록 조회 성공", PageResponse.from(response)));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<BaseResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "읽지 않은 알림 수 조회 성공", count));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<BaseResponse<Void>> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userId, notificationId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "알림 읽음 처리 성공", null));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<BaseResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK, "전체 알림 읽음 처리 성공", null));
    }
}
