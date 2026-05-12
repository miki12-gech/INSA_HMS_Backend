package com.insa.hospital.repository;

import com.insa.hospital.entity.PaymentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentCategoryRepository extends JpaRepository<PaymentCategory, Integer> {
    /** All categories for hospital — used for billing dropdown. */
    List<PaymentCategory> findByHospitalId(String hospitalId);
    List<PaymentCategory> findByTypeAndHospitalId(String type, String hospitalId);
}
