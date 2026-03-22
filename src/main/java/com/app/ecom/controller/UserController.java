package com.app.ecom.controller;

import com.app.ecom.dto.UserRequest;
import com.app.ecom.dto.UserResponse;
import com.app.ecom.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

//    @GetMapping("/")
//    public ResponseEntity<String> getBaseCase() {
//        return new ResponseEntity<>("It's working...", HttpStatus.OK);
//    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("Fetching all users");
        return ResponseEntity.ok(userService.fetchAllUsers());
    }

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody UserRequest userRequest) {
        log.info("Create user request received for email={}", userRequest.getEmail());
        userService.addUser(userRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body("User created");
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable("userId") @Positive(message = "userId must be positive") Long id) {
        log.info("Fetching user by id={}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<String> updateUser(@PathVariable("userId") @Positive(message = "userId must be positive") Long id,
                                             @Valid @RequestBody UserRequest updatedUser) {
        log.info("Update user request received for id={}", id);
        userService.updateUser(id, updatedUser);
        log.info("User updated successfully for id={}", id);
        return ResponseEntity.ok("User updated");
    }
}
