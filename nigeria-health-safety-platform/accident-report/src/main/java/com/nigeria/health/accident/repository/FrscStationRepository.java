package com.nigeria.health.accident.repository;

import com.nigeria.health.accident.entity.FrscStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FrscStationRepository extends JpaRepository<FrscStation, UUID> {

    /** Find the nearest FRSC station — same LGA preferred */
    Optional<FrscStation> findFirstByStateAndLga(String state, String lga);

    /** Fallback: any station in the same state */
    Optional<FrscStation> findFirstByState(String state);

    List<FrscStation> findByState(String state);
}
