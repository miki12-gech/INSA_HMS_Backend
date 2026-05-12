package com.insa.hospital.repository;

import com.insa.hospital.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, Integer> {
    Optional<PaymentGateway> findByHospitalIdAndName(String hospitalId, String name);
}
