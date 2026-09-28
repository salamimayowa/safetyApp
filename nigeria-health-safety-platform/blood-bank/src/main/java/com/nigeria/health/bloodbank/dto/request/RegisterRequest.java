package com.nigeria.health.bloodbank.dto.request;

import com.nigeria.health.shared.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO: RegisterRequest
 * Description: Payload for creating a new user account.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+234|0)[789][01]\\d{8}$",
             message = "Phone must be a valid Nigerian number e.g. 08012345678")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    private String state;
    private String lga;
}
