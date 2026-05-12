package com.insa.hospital.service;

import com.insa.hospital.entity.MedicineCategory;
import com.insa.hospital.entity.MedicineCategory1;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.MedicineCategory1Repository;
import com.insa.hospital.repository.MedicineCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MedicineCategoryService {

    private final MedicineCategoryRepository catRepo;
    private final MedicineCategory1Repository cat1Repo;

    @Autowired
    public MedicineCategoryService(MedicineCategoryRepository catRepo, MedicineCategory1Repository cat1Repo) {
        this.catRepo = catRepo;
        this.cat1Repo = cat1Repo;
    }

    // ─── Primary Categories ───────────────────────────────────────────────────

    public List<MedicineCategory> listPrimaryCategories(String hospitalId) {
        return catRepo.findByHospitalId(hospitalId);
    }

    public MedicineCategory getPrimaryCategory(Integer id, String hospitalId) {
        return catRepo.findById(id)
                .filter(c -> hospitalId.equals(c.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("MedicineCategory", "id", id));
    }

    public MedicineCategory createPrimaryCategory(MedicineCategory category, String hospitalId) {
        category.setHospitalId(hospitalId);
        return catRepo.save(category);
    }

    public MedicineCategory updatePrimaryCategory(Integer id, MedicineCategory category, String hospitalId) {
        MedicineCategory existing = getPrimaryCategory(id, hospitalId);
        existing.setCategory(category.getCategory());
        existing.setDescription(category.getDescription());
        return catRepo.save(existing);
    }

    public void deletePrimaryCategory(Integer id, String hospitalId) {
        catRepo.delete(getPrimaryCategory(id, hospitalId));
    }

    // ─── Secondary Categories (Category1) ─────────────────────────────────────

    public List<MedicineCategory1> listSecondaryCategories(String hospitalId) {
        return cat1Repo.findByHospitalId(hospitalId);
    }

    public MedicineCategory1 getSecondaryCategory(Integer id, String hospitalId) {
        return cat1Repo.findById(id)
                .filter(c -> hospitalId.equals(c.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("MedicineCategory1", "id", id));
    }

    public MedicineCategory1 createSecondaryCategory(MedicineCategory1 category, String hospitalId) {
        category.setHospitalId(hospitalId);
        return cat1Repo.save(category);
    }

    public MedicineCategory1 updateSecondaryCategory(Integer id, MedicineCategory1 category, String hospitalId) {
        MedicineCategory1 existing = getSecondaryCategory(id, hospitalId);
        existing.setCategory(category.getCategory());
        existing.setDescription(category.getDescription());
        return cat1Repo.save(existing);
    }

    public void deleteSecondaryCategory(Integer id, String hospitalId) {
        cat1Repo.delete(getSecondaryCategory(id, hospitalId));
    }
}
