package com.sudswastik.identityservice.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 255) String name
) {}
