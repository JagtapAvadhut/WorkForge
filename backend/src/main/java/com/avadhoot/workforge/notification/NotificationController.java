package com.avadhoot.workforge.notification;

import com.avadhoot.workforge.common.dto.ApiResponse;
import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.notification.dto.NotificationResponse;
import com.avadhoot.workforge.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(Pageable pageable) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(notificationService.list(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.success(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markReadPatch(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.requireCurrentUserId(), id);
        return ApiResponse.message("Notification marked as read");
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markReadPost(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.requireCurrentUserId(), id);
        return ApiResponse.message("Notification marked as read");
    }

    @PatchMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllReadPatch() {
        notificationService.markAllRead(SecurityUtils.requireCurrentUserId());
        return ApiResponse.success(Map.of("updated", 1), "All notifications marked as read");
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllReadPost() {
        notificationService.markAllRead(SecurityUtils.requireCurrentUserId());
        return ApiResponse.success(Map.of("updated", 1), "All notifications marked as read");
    }
}
