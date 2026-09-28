package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.BloodRequest;
import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {

    Page<BloodRequest> findByStatus(String status, Pageable pageable);

    Page<BloodRequest> findByRequestingHospitalId(UUID hospitalId, Pageable pageable);

    /**
     * Find CRITICAL or URGENT requests that have been OPEN for more than 2 hours.
     * Used by the escalation scheduler to alert nearby hospitals.
     */
    @Query("""
        SELECT br FROM BloodRequest br
        WHERE br.status = 'OPEN'
        AND br.urgency IN :urgencies
        AND br.createdAt <= :cutoffTime
        """)
    List<BloodRequest> findUnfulfilledUrgentRequests(
            @Param("urgencies") List<UrgencyLevel> urgencies,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find open requests that have passed their expiry time.
     * Used by scheduler to auto-expire stale requests.
     */
    @Query("""
        SELECT br FROM BloodRequest br
        WHERE br.status = 'OPEN'
        AND br.expiresAt < :now
        """)
    List<BloodRequest> findExpiredRequests(@Param("now") LocalDateTime now);

    Page<BloodRequest> findByRequestingHospitalIdAndStatus(
            UUID hospitalId, String status, Pageable pageable);
}
