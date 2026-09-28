package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.Donation;
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
public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Page<Donation> findByDonorId(UUID donorId, Pageable pageable);

    Page<Donation> findByHospitalId(UUID hospitalId, Pageable pageable);

    /** Find appointments scheduled for the next 24 hours — for reminder emails. */
    @Query("""
        SELECT d FROM Donation d
        WHERE d.status = 'SCHEDULED'
        AND d.appointmentDate BETWEEN :from AND :to
        """)
    List<Donation> findUpcomingAppointments(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
