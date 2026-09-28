package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.Donor;
import com.nigeria.health.shared.enums.BloodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonorRepository extends JpaRepository<Donor, UUID> {

    Optional<Donor> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Find donors who became eligible to donate again today (exactly 56 days after last donation).
     * Used by the daily eligibility reminder scheduler at 8am.
     */
    @Query("""
        SELECT d FROM Donor d
        WHERE d.lastDonationDate = :eligibilityDate
        AND d.isEligible = false
        """)
    List<Donor> findDonorsEligibleToday(@Param("eligibilityDate") LocalDate eligibilityDate);

    List<Donor> findByBloodTypeAndStateAndIsEligibleTrue(BloodType bloodType, String state);

    List<Donor> findByBloodTypeAndStateAndLgaAndIsEligibleTrue(
            BloodType bloodType, String state, String lga);
}
