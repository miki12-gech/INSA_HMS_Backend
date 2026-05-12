package com.insa.hospital.service;

import com.insa.hospital.entity.BloodBank;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.BloodBankRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Blood Bank Service — manages blood stock levels in the legacy `bankb` table.
 *
 * Key behaviour:
 *  - Listing returns all blood groups for the hospital.
 *  - Update allows setting the stock status (e.g. "5 Bags").
 */
@Service
@Transactional
public class BloodBankService {

    private final BloodBankRepository bloodBankRepository;

    @Autowired
    public BloodBankService(BloodBankRepository bloodBankRepository) {
        this.bloodBankRepository = bloodBankRepository;
    }

    @Transactional(readOnly = true)
    public List<BloodBank> listAll(String hospitalId) {
        return bloodBankRepository.findByHospitalId(hospitalId);
    }

    @Transactional(readOnly = true)
    public BloodBank getById(Integer id, String hospitalId) {
        return bloodBankRepository.findById(id)
                .filter(b -> hospitalId.equals(b.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("BloodBank", "id", id));
    }

    public BloodBank create(BloodBank bloodBank, String hospitalId) {
        bloodBank.setHospitalId(hospitalId);
        return bloodBankRepository.save(bloodBank);
    }

    /**
     * Update the stock status for a blood bank entry.
     * e.g. set status = "5 Bags"
     */
    public BloodBank updateStatus(Integer id, String status, String hospitalId) {
        BloodBank existing = getById(id, hospitalId);
        existing.setStatus(status);
        return bloodBankRepository.save(existing);
    }

    public BloodBank update(Integer id, BloodBank updated, String hospitalId) {
        BloodBank existing = getById(id, hospitalId);
        existing.setGroup(updated.getGroup());
        existing.setStatus(updated.getStatus());
        return bloodBankRepository.save(existing);
    }

    public void delete(Integer id, String hospitalId) {
        bloodBankRepository.delete(getById(id, hospitalId));
    }
}
