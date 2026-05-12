package com.insa.hospital.service;

import com.insa.hospital.entity.AllotedBed;
import com.insa.hospital.entity.Bed;
import com.insa.hospital.entity.BedCategory;
import com.insa.hospital.repository.AllotedBedRepository;
import com.insa.hospital.repository.BedCategoryRepository;
import com.insa.hospital.repository.BedRepository;
import com.insa.hospital.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BedService {

    private final BedRepository bedRepository;
    private final BedCategoryRepository bedCategoryRepository;
    private final AllotedBedRepository allotedBedRepository;

    private String getHospitalId() {
        return JwtContextHolder.getHospitalId();
    }

    @Transactional(readOnly = true)
    public List<Bed> getBeds() {
        return bedRepository.findByHospitalId(getHospitalId());
    }

    @Transactional(readOnly = true)
    public Bed getBedById(Long id) {
        return bedRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElse(null);
    }

    @Transactional
    public void saveBed(Bed bed) {
        bed.setHospitalId(getHospitalId());
        bed.setBedId(bed.getCategory() + "-" + bed.getNumber());
        bedRepository.save(bed);
    }

    @Transactional
    public void updateBed(Long id, Bed updated) {
        Bed existing = bedRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElseThrow(() -> new RuntimeException("Bed not found"));
        existing.setCategory(updated.getCategory());
        existing.setNumber(updated.getNumber());
        existing.setDescription(updated.getDescription());
        existing.setBedId(updated.getCategory() + "-" + updated.getNumber());
        if(updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        bedRepository.save(existing);
    }

    @Transactional
    public void deleteBed(Long id) {
        bedRepository.findByIdAndHospitalId(id, getHospitalId())
                .ifPresent(bedRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<BedCategory> getBedCategories() {
        return bedCategoryRepository.findByHospitalId(getHospitalId());
    }

    @Transactional(readOnly = true)
    public BedCategory getBedCategoryById(Long id) {
        return bedCategoryRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElse(null);
    }

    @Transactional
    public void saveBedCategory(BedCategory category) {
        category.setHospitalId(getHospitalId());
        bedCategoryRepository.save(category);
    }

    @Transactional
    public void updateBedCategory(Long id, BedCategory updated) {
        BedCategory existing = bedCategoryRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        bedCategoryRepository.save(existing);
    }

    @Transactional
    public void deleteBedCategory(Long id) {
        bedCategoryRepository.findByIdAndHospitalId(id, getHospitalId())
                .ifPresent(bedCategoryRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<AllotedBed> getAllotedBeds() {
        return allotedBedRepository.findByHospitalId(getHospitalId());
    }

    @Transactional(readOnly = true)
    public AllotedBed getAllotedBedById(Long id) {
        return allotedBedRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElse(null);
    }

    @Transactional
    public void saveAllotedBed(AllotedBed alloted) {
        alloted.setHospitalId(getHospitalId());
        allotedBedRepository.save(alloted);

        // Simultaneous update to bed
        updateBedTimes(alloted.getBedId(), alloted.getATime(), alloted.getDTime());
    }

    @Transactional
    public void updateAllotedBed(Long id, AllotedBed updated) {
        AllotedBed existing = allotedBedRepository.findByIdAndHospitalId(id, getHospitalId())
                .orElseThrow(() -> new RuntimeException("Alloted Bed not found"));
        existing.setPatient(updated.getPatient());
        existing.setATime(updated.getATime());
        existing.setDTime(updated.getDTime());
        existing.setBedId(updated.getBedId());
        existing.setStatus(updated.getStatus());
        allotedBedRepository.save(existing);

        // Simultaneous update to bed
        updateBedTimes(updated.getBedId(), updated.getATime(), updated.getDTime());
    }

    @Transactional
    public void deleteAllotedBed(Long id) {
        allotedBedRepository.findByIdAndHospitalId(id, getHospitalId())
                .ifPresent(allotedBedRepository::delete);
    }

    private void updateBedTimes(String bedId, String aTime, String dTime) {
        if (bedId == null || bedId.isEmpty()) return;
        bedRepository.findByBedIdAndHospitalId(bedId, getHospitalId())
                .ifPresent(bed -> {
                    bed.setLastATime(aTime);
                    bed.setLastDTime(dTime);
                    bedRepository.save(bed);
                });
    }
}
