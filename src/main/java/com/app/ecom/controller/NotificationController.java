package com.app.ecom.controller;

import com.app.ecom.dto.NotificationResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.NotificationService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Fetch notifications request received for userId={}", currentUser.getId());
        return ResponseEntity.ok(notificationService.getNotifications(currentUser.getId()));
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @PathVariable @Positive(message = "id must be positive") Long id) {
        log.info("Mark notification read request received for userId={}, notificationId={}", currentUser.getId(), id);
        return ResponseEntity.ok(notificationService.markAsRead(id, currentUser.getId()));
    }
}
