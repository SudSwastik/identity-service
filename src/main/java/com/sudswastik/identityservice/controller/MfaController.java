package com.sudswastik.identityservice.controller;

import com.sudswastik.identityservice.dto.auth.MessageResponse;
import com.sudswastik.identityservice.dto.mfa.MfaSetupResponse;
import com.sudswastik.identityservice.dto.mfa.MfaStatusResponse;
import com.sudswastik.identityservice.dto.mfa.VerifyMfaSetupRequest;
import com.sudswastik.identityservice.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setup(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("cognito:username");
        if (username == null) username = jwt.getSubject();
        return ResponseEntity.ok(mfaService.setupMfa(jwt.getTokenValue(), username));
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<MessageResponse> verifySetup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyMfaSetupRequest request) {
        return ResponseEntity.ok(
                mfaService.verifyMfaSetup(jwt.getTokenValue(), request.userCode(), jwt.getSubject()));
    }

    @DeleteMapping("/disable")
    public ResponseEntity<MessageResponse> disable(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(mfaService.disableMfa(jwt.getSubject()));
    }

    @GetMapping("/status")
    public ResponseEntity<MfaStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(mfaService.getMfaStatus(jwt.getSubject()));
    }
}
