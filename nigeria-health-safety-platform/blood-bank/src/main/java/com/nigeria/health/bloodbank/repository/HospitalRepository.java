package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.Hospital;
import com.nigeria.health.shared.enums.BloodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    Page<Hospital> findByIsApprovedTrue(Pageable pageable);

    Page<Hospital> findByIsApprovedFalse(Pageable pageable);

    Optional<Hospital> findFirstByAdminUserId(UUID adminUserId);

    List<Hospital> findByStateAndIsApprovedTrue(String state);

    List<Hospital> findByStateAndLgaAndIsApprovedTrue(String state, String lga);

    /**
     * Find hospitals in same LGA with available stock of a specific blood type.
     * Used for proximity matching — LGA-level results returned first.
     */
    @Query("""
        SELECT h FROM Hospital h
        JOIN h.bloodStock bs
        WHERE h.isApproved = true
        AND h.state = :state
        AND h.lga = :lga
        AND bs.bloodType = :bloodType
        AND bs.unitsAvailable > 0
        """)
    List<Hospital> findHospitalsInLgaWithBlood(
            @Param("state") String state,
            @Param("lga") String lga,
            @Param("bloodType") BloodType bloodType);

    /**
     * Find hospitals in same state (any LGA) with available stock.
     * Used when no LGA-level match is found.
     */
    @Query("""
        SELECT h FROM Hospital h
        JOIN h.bloodStock bs
        WHERE h.isApproved = true
        AND h.state = :state
        AND bs.bloodType = :bloodType
        AND bs.unitsAvailable > 0
        """)
    List<Hospital> findHospitalsInStateWithBlood(
            @Param("state") String state,
            @Param("bloodType") BloodType bloodType);

    /**
     * Find all approved hospitals with admin email for notifications.
     * Used for critical blood request broadcasts.
     */
    @Query("""
        SELECT h FROM Hospital h
        WHERE h.isApproved = true
        AND h.state = :state
        """)
    List<Hospital> findAllApprovedInState(@Param("state") String state);
}
