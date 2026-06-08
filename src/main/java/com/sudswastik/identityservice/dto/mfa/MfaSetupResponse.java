package com.sudswastik.identityservice.dto.mfa;

public record MfaSetupResponse(String secretCode, String qrCodeUri) {}
