package com.app.ecom.controller;

import com.app.ecom.dto.ReturnCreateRequest;
import com.app.ecom.dto.ReturnResponse;
import com.app.ecom.security.AppUserDetails;
import com.app.ecom.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponse> createReturn(
            @AuthenticationPrincipal AppUserDetails currentUser,
            @Valid @RequestBody ReturnCreateRequest request) {
        log.info("Create return request received for userId={}, orderId={}", currentUser.getId(), request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnService.createReturn(currentUser.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<ReturnResponse>> getReturns(
            @AuthenticationPrincipal AppUserDetails currentUser) {
        log.info("Fetch returns request received for userId={}", currentUser.getId());
        return ResponseEntity.ok(returnService.getReturns(currentUser.getId()));
    }
}
