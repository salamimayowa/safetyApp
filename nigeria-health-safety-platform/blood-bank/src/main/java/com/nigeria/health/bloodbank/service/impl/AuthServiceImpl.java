package com.nigeria.health.bloodbank.service.impl;

import com.nigeria.health.bloodbank.dto.request.*;
import com.nigeria.health.bloodbank.dto.response.AuthResponse;
import com.nigeria.health.bloodbank.entity.OtpToken;
import com.nigeria.health.bloodbank.entity.User;
import com.nigeria.health.bloodbank.repository.OtpTokenRepository;
import com.nigeria.health.bloodbank.repository.UserRepository;
import com.nigeria.health.shared.email.EmailService;
import com.nigeria.health.shared.enums.OtpPurpose;
import com.nigeria.health.shared.exception.BadRequestException;
import com.nigeria.health.shared.exception.ConflictException;
import com.nigeria.health.shared.exception.ResourceNotFoundException;
import com.nigeria.health.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service: AuthServiceImpl
 * Description: Handles user registration, OTP email verification,
 *              login (JWT issuance), password reset via OTP, and password change.
 * Props: none
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    // ─── REGISTRATION ────────────────────────────────────────────────

    /**
     * Register a new user account.
     * Sends a 6-digit OTP to their email for verification.
     * Account is not active until email is verified.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("An account with this phone number already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .state(request.getState())
                .lga(request.getLga())
                .isVerified(false)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} with role: {}", user.getEmail(), user.getRole());

        sendOtp(user, OtpPurpose.EMAIL_VERIFICATION);
    }

    // ─── OTP VERIFICATION ────────────────────────────────────────────

    /**
     * Verify email using the OTP sent after registration.
     * Marks user as verified and sends a welcome email.
     */
    @Transactional
    public void verifyEmail(OtpVerifyRequest request) {
        User user = findUserByEmail(request.getEmail());

        if (user.getIsVerified()) {
            throw new BadRequestException("This account is already verified");
        }

        validateAndConsumeOtp(user, request.getOtp(), OtpPurpose.EMAIL_VERIFICATION);

        user.setIsVerified(true);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getEmail());

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName(),
                user.getRole().name());
    }

    /**
     * Resend OTP to a user's email. Invalidates any previous OTPs first.
     */
    @Transactional
    public void resendOtp(String email, OtpPurpose purpose) {
        User user = findUserByEmail(email);
        sendOtp(user, purpose);
        log.info("OTP resent to: {} for purpose: {}", email, purpose);
    }

    // ─── LOGIN ───────────────────────────────────────────────────────

    /**
     * Authenticate user with email + password.
     * Returns JWT access and refresh tokens.
     */
    public AuthResponse login(LoginRequest request) {
        // Spring Security validates credentials — throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = findUserByEmail(request.getEmail());

        if (!user.getIsVerified()) {
            throw new BadRequestException(
                    "Please verify your email before logging in. " +
                    "Check your inbox for the OTP we sent.");
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getEmail(), user.getId());

        log.info("User logged in: {} | Role: {}", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .build();
    }

    // ─── PASSWORD RESET ──────────────────────────────────────────────

    /**
     * Send a password reset OTP to the user's email.
     * Works even if user hasn't verified their email yet.
     */
    @Transactional
    public void forgotPassword(String email) {
        // Always return success even if email not found — prevents email enumeration attacks
        userRepository.findByEmail(email.toLowerCase().trim())
                .ifPresent(user -> sendOtp(user, OtpPurpose.PASSWORD_RESET));
        log.info("Password reset OTP requested for: {}", email);
    }

    /**
     * Reset password using the OTP received by email.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = findUserByEmail(request.getEmail());

        validateAndConsumeOtp(user, request.getOtp(), OtpPurpose.PASSWORD_RESET);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getEmail());

        emailService.sendEmail(user.getEmail(),
                "Password Changed Successfully",
                "password-changed",
                java.util.Map.of("userName", user.getFullName()));
    }

    // ─── INTERNAL HELPERS ────────────────────────────────────────────

    /**
     * Generate a 6-digit OTP, save it to DB, and email it to the user.
     * Invalidates any previous OTPs for the same user+purpose first.
     */
    private void sendOtp(User user, OtpPurpose purpose) {
        // Invalidate previous OTPs of same type
        otpTokenRepository.invalidateAllForUser(user.getId(), purpose);

        // Generate new 6-digit OTP
        String otpCode = String.format("%06d", secureRandom.nextInt(999999));

        OtpToken otpToken = OtpToken.builder()
                .user(user)
                .token(otpCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .isUsed(false)
                .attemptCount(0)
                .build();

        otpTokenRepository.save(otpToken);

        emailService.sendOtpEmail(
                user.getEmail(),
                user.getFullName(),
                otpCode,
                formatPurpose(purpose),
                otpExpiryMinutes
        );

        log.info("OTP sent to: {} for purpose: {}", user.getEmail(), purpose);
    }

    /**
     * Validate an OTP against the DB and mark it as used if valid.
     * Throws BadRequestException for expired, invalid, or over-attempted OTPs.
     */
    private void validateAndConsumeOtp(User user, String inputOtp, OtpPurpose purpose) {
        OtpToken otpToken = otpTokenRepository
                .findValidToken(user.getId(), purpose, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException(
                        "OTP has expired or is invalid. Please request a new one."));

        if (otpToken.hasExceededAttempts()) {
            throw new BadRequestException(
                    "Too many incorrect attempts. Please request a new OTP.");
        }

        if (!otpToken.getToken().equals(inputOtp)) {
            otpToken.incrementAttempt();
            otpTokenRepository.save(otpToken);
            int remaining = 3 - otpToken.getAttemptCount();
            throw new BadRequestException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // Mark OTP as used so it cannot be reused
        otpToken.setIsUsed(true);
        otpTokenRepository.save(otpToken);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + email));
    }

    private String formatPurpose(OtpPurpose purpose) {
        return switch (purpose) {
            case EMAIL_VERIFICATION -> "Email Verification";
            case PASSWORD_RESET -> "Password Reset";
            case LOGIN_2FA -> "Two-Factor Authentication";
        };
    }
}
