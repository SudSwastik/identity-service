package com.sudswastik.identityservice.controller;

import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.user.UpdateProfileRequest;
import com.sudswastik.identityservice.dto.user.UserProfileResponse;
import com.sudswastik.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getProfile(jwt.getSubject()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(jwt.getSubject(), request));
    }

    @GetMapping("/{sub}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> getUser(@PathVariable String sub) {
        return ResponseEntity.ok(userService.getProfile(sub));
    }

    @PostMapping("/{sub}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> disableUser(@PathVariable String sub) {
        userService.disableUser(sub);
        return ResponseEntity.ok(new MessageResponse("User disabled successfully."));
    }

    @PostMapping("/{sub}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> enableUser(@PathVariable String sub) {
        userService.enableUser(sub);
        return ResponseEntity.ok(new MessageResponse("User enabled successfully."));
    }
}
