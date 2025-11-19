package com.tracking.repository;

import com.tracking.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Integer> {

    Optional<Carrier> findByCarrierKey(String carrierKey);

    List<Carrier> findByCountry(String country);

    List<Carrier> findByIsActive(Boolean isActive);

    @Query("SELECT c FROM Carrier c WHERE c.isActive = true ORDER BY c.carrierName")
    List<Carrier> findAllActive();

    boolean existsByCarrierCode(Integer carrierCode);
}
