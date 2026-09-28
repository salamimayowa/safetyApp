package com.nigeria.health.bloodbank.dto.response;

import com.nigeria.health.shared.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO: AuthResponse
 * Description: Returned on successful login — contains JWT tokens and basic user info.
 *              The frontend stores accessToken in memory and refreshToken in secure storage.
 */
@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UUID userId;
    private String fullName;
    private String email;
    private Role role;
    private Boolean isVerified;
}
