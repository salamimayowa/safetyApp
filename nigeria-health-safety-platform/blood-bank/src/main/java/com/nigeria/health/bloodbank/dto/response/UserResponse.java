package com.nigeria.health.bloodbank.dto.response;

import com.nigeria.health.shared.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String state;
    private String lga;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
