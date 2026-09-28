package com.nigeria.health.bloodbank.repository;

import com.nigeria.health.bloodbank.entity.BloodStock;
import com.nigeria.health.shared.enums.BloodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodStockRepository extends JpaRepository<BloodStock, UUID> {

    List<BloodStock> findByHospitalId(UUID hospitalId);

    Optional<BloodStock> findByHospitalIdAndBloodType(UUID hospitalId, BloodType bloodType);

    /** Find all blood stock records below the low-stock threshold (2 units). */
    @Query("""
        SELECT bs FROM BloodStock bs
        WHERE bs.hospital.isApproved = true
        AND bs.unitsAvailable < 2
        AND bs.unitsAvailable > 0
        """)
    List<BloodStock> findAllLowStockRecords();
}
