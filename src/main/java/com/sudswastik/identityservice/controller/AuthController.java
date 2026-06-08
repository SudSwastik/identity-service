package com.sudswastik.identityservice.controller;

import com.sudswastik.identityservice.dto.auth.AuthResponse;
import com.sudswastik.identityservice.dto.auth.ForgotPasswordRequest;
import com.sudswastik.identityservice.dto.auth.LoginRequest;
import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.auth.MfaChallengeRequest;
import com.sudswastik.identityservice.dto.auth.RefreshTokenRequest;
import com.sudswastik.identityservice.dto.auth.RegisterRequest;
import com.sudswastik.identityservice.dto.auth.ResendVerificationRequest;
import com.sudswastik.identityservice.dto.auth.ResetPasswordRequest;
import com.sudswastik.identityservice.dto.auth.VerifyEmailRequest;
import com.sudswastik.identityservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/mfa-challenge")
    public ResponseEntity<AuthResponse> respondToMfaChallenge(@Valid @RequestBody MfaChallengeRequest request) {
        return ResponseEntity.ok(authService.respondToMfaChallenge(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String accessToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        return ResponseEntity.ok(authService.logout(accessToken));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerificationCode(request));
    }
}
