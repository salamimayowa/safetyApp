package com.nigeria.health.bloodbank.entity;

import com.nigeria.health.shared.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: OtpToken
 * Description: Stores one-time passwords for email verification and password reset.
 *              Each OTP expires after 10 minutes.
 *              After 3 failed attempts the user must request a new OTP.
 * Props: none
 */
@Entity
@Table(name = "otp_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 6)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // ─── Business logic helpers ─────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid(String inputToken) {
        return !isUsed && !isExpired() && token.equals(inputToken);
    }

    public void incrementAttempt() {
        this.attemptCount++;
    }

    public boolean hasExceededAttempts() {
        return attemptCount >= 3;
    }
}
