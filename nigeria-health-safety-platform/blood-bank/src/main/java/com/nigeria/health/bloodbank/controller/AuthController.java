package com.nigeria.health.bloodbank.controller;

import com.nigeria.health.bloodbank.dto.request.*;
import com.nigeria.health.bloodbank.dto.response.AuthResponse;
import com.nigeria.health.bloodbank.service.impl.AuthServiceImpl;
import com.nigeria.health.shared.enums.OtpPurpose;
import com.nigeria.health.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller: AuthController
 * Description: All authentication endpoints — register, verify OTP, login, password reset.
 *              All endpoints here are PUBLIC (no JWT required).
 * Props: none
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, verify, login, and reset password")
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful! Please check your email for a verification code."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address using OTP")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Email verified successfully. You can now log in."));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP for email verification")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @RequestParam @Email String email) {
        authService.resendOtp(email, OtpPurpose.EMAIL_VERIFICATION);
        return ResponseEntity.ok(ApiResponse.success(
                "A new verification code has been sent to " + email));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset OTP")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam @Email String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with that email, a reset code has been sent."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successfully. Please log in with your new password."));
    }
}
