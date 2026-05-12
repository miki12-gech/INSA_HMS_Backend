package com.insa.hospital.controller;

import com.insa.hospital.dto.MedicineDto;
import com.insa.hospital.dto.MedicineIssueRequestDto;
import com.insa.hospital.dto.PharmacyPosRequestDto;
import com.insa.hospital.dto.SmartDispenseRequestDto;
import com.insa.hospital.entity.PharmacyPayment;
import com.insa.hospital.entity.PharmacyExpense;
import com.insa.hospital.entity.PharmacyExpenseCategory;
import com.insa.hospital.entity.MedicineIssue;
import com.insa.hospital.repository.PharmacyPaymentRepository;
import com.insa.hospital.repository.PharmacyExpenseRepository;
import com.insa.hospital.repository.PharmacyExpenseCategoryRepository;
import com.insa.hospital.repository.MedicineIssueRepository;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.PharmacyService;
import com.insa.hospital.service.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PharmacyController {

    @Autowired
    private PharmacyService pharmacyService;

    @Autowired
    private PharmacyPaymentRepository pharmacyPaymentRepository;

    @Autowired
    private PharmacyExpenseRepository pharmacyExpenseRepository;

    @Autowired
    private PharmacyExpenseCategoryRepository pharmacyExpenseCategoryRepository;

    @Autowired
    private MedicineIssueRepository medicineIssueRepository;

    @Autowired
    private TenantAccessService tenantAccessService;

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin')")
    public ResponseEntity<List<MedicineDto>> getInventory() {
        return ResponseEntity.ok(pharmacyService.getAllMedicines());
    }

    @PostMapping("/dispense")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin')")
    public ResponseEntity<String> smartDispense(@RequestBody SmartDispenseRequestDto request) {
        pharmacyService.processSmartDispense(request);
        return ResponseEntity.ok("Smart Dispense completed successfully");
    }

    // --- POS PAYMENT ENDPOINTS ---

    @GetMapping("/payment")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<List<PharmacyPayment>> getPayments(@RequestParam(required = false) String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return ResponseEntity.ok(pharmacyPaymentRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        }
        return ResponseEntity.ok(pharmacyPaymentRepository.findByHospitalIdOrderByIdDesc(JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/payment")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<PharmacyPayment> addPayment(@RequestBody PharmacyPosRequestDto request) {
        return ResponseEntity.ok(pharmacyService.addPayment(request));
    }

    @DeleteMapping("/payment/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deletePayment(@PathVariable Long id) {
        pharmacyPaymentRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // --- EXPENSE ENDPOINTS ---

    @GetMapping("/expense")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<List<PharmacyExpense>> getExpenses(@RequestParam(required = false) String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return ResponseEntity.ok(pharmacyExpenseRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        }
        return ResponseEntity.ok(pharmacyExpenseRepository.findByHospitalIdOrderByIdDesc(JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/expense")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<PharmacyExpense> addExpense(@RequestBody PharmacyExpense expense) {
        if(expense.getDate() == null) {
            expense.setDate(String.valueOf(System.currentTimeMillis() / 1000));
        }
        if (!tenantAccessService.isSuperAdmin() || expense.getHospitalId() == null || expense.getHospitalId().isBlank()) {
            expense.setHospitalId(tenantAccessService.getCurrentHospitalId());
        }
        return ResponseEntity.ok(pharmacyExpenseRepository.save(expense));
    }

    @DeleteMapping("/expense/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deleteExpense(@PathVariable Long id) {
        pharmacyExpenseRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // --- EXPENSE CATEGORY ENDPOINTS ---

    @GetMapping("/expense-category")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<List<PharmacyExpenseCategory>> getExpenseCategories(@RequestParam(required = false) String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return ResponseEntity.ok(pharmacyExpenseCategoryRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        }
        return ResponseEntity.ok(pharmacyExpenseCategoryRepository.findByHospitalIdOrderByIdDesc(JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/expense-category")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<PharmacyExpenseCategory> addExpenseCategory(@RequestBody PharmacyExpenseCategory category) {
        if (!tenantAccessService.isSuperAdmin() || category.getHospitalId() == null || category.getHospitalId().isBlank()) {
            category.setHospitalId(tenantAccessService.getCurrentHospitalId());
        }
        return ResponseEntity.ok(pharmacyExpenseCategoryRepository.save(category));
    }

    @DeleteMapping("/expense-category/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deleteExpenseCategory(@PathVariable Long id) {
        pharmacyExpenseCategoryRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }

    // --- MEDICINE ISSUE ENDPOINTS ---

    @GetMapping("/medicine-issue")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<List<MedicineIssue>> getMedicineIssues(@RequestParam(required = false) String hospitalId) {
        if (tenantAccessService.isSuperAdmin()) {
            return ResponseEntity.ok(medicineIssueRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")));
        }
        return ResponseEntity.ok(medicineIssueRepository.findByHospitalIdOrderByIdDesc(JwtContextHolder.getHospitalId()));
    }

    @PostMapping("/medicine-issue")
    @PreAuthorize("hasAnyAuthority('Pharmacist', 'admin', 'superadmin', 'accountant')")
    public ResponseEntity<MedicineIssue> addMedicineIssue(@RequestBody MedicineIssueRequestDto request) {
        return ResponseEntity.ok(pharmacyService.addMedicineIssue(request));
    }

    @DeleteMapping("/medicine-issue/{id}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> deleteMedicineIssue(@PathVariable Long id) {
        medicineIssueRepository.deleteById(id);
        return ResponseEntity.ok("Deleted");
    }
}

