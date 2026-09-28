package com.nigeria.health.bloodbank.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain uppercase, lowercase and a number")
    private String newPassword;
}
