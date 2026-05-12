package com.insa.hospital.service;

import com.insa.hospital.entity.Donor;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Donor Service — Business Logic for the Blood Donor module.
 */
@Service
@Transactional
public class DonorService {

    private static final DateTimeFormatter ADD_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yy");

    private final DonorRepository donorRepository;

    @Autowired
    public DonorService(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    public Donor createDonor(Donor donor, String hospitalId) {
        donor.setHospitalId(hospitalId);
        if (!StringUtils.hasText(donor.getAddDate())) {
            donor.setAddDate(LocalDate.now().format(ADD_DATE_FMT));
        }
        return donorRepository.save(donor);
    }

    @Transactional(readOnly = true)
    public Page<Donor> listAll(String hospitalId, Pageable pageable) {
        return donorRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Donor> listByGroup(String group, String hospitalId, Pageable pageable) {
        return donorRepository.findByGroupAndHospitalId(group, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Donor getById(Integer id, String hospitalId) {
        return donorRepository.findById(id)
                .filter(d -> hospitalId.equals(d.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Donor", "id", id));
    }

    public Donor update(Integer id, Donor updated, String hospitalId) {
        Donor existing = getById(id, hospitalId);
        existing.setName(updated.getName());
        existing.setGroup(updated.getGroup());
        existing.setAge(updated.getAge());
        existing.setSex(updated.getSex());
        existing.setLdd(updated.getLdd());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        return donorRepository.save(existing);
    }

    public void delete(Integer id, String hospitalId) {
        donorRepository.delete(getById(id, hospitalId));
    }
}
