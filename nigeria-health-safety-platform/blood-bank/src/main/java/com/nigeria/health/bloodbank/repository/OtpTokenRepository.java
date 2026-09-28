package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.OtpToken;
import com.nigeria.health.shared.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    /**
     * Find the most recent unused, unexpired OTP for a user and purpose.
     */
    @Query("""
        SELECT o FROM OtpToken o
        WHERE o.user.id = :userId
        AND o.purpose = :purpose
        AND o.isUsed = false
        AND o.expiresAt > :now
        ORDER BY o.createdAt DESC
        """)
    Optional<OtpToken> findValidToken(
            @Param("userId") UUID userId,
            @Param("purpose") OtpPurpose purpose,
            @Param("now") LocalDateTime now);

    /**
     * Invalidate all previous OTPs for a user+purpose before issuing a new one.
     * Prevents multiple valid OTPs existing simultaneously.
     */
    @Modifying
    @Query("""
        UPDATE OtpToken o
        SET o.isUsed = true
        WHERE o.user.id = :userId
        AND o.purpose = :purpose
        AND o.isUsed = false
        """)
    void invalidateAllForUser(
            @Param("userId") UUID userId,
            @Param("purpose") OtpPurpose purpose);
}
