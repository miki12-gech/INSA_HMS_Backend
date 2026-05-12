package com.insa.hospital.controller;

import com.insa.hospital.entity.Expense;
import com.insa.hospital.entity.ExpenseCategory;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expense REST Controller
 * Base URLs:
 *   /api/expenses         — Expense records
 *   /api/expenses/categories — Expense Category records
 *
 * Roles:
 *  - View: Accountant, admin, superadmin
 *  - Create/Update/Delete: Accountant, admin, superadmin
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    // ═══════════════════════════════════════════════════════════
    // EXPENSE ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Expense> createExpense(
            @RequestBody Expense expense,
            Authentication authentication) {
        String hospitalId = JwtContextHolder.getHospitalId();
        String userId = JwtContextHolder.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(expense, hospitalId, userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Page<Expense>> listExpenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) String category,
            Authentication authentication) {

        String hospitalId = JwtContextHolder.getHospitalId();
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Expense> result = StringUtils.hasText(category)
                ? expenseService.listExpensesByCategory(category, hospitalId, pageable)
                : expenseService.listExpenses(hospitalId, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Expense> getExpenseById(
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(
                expenseService.getExpenseById(id, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Expense> updateExpense(
            @PathVariable Integer id,
            @RequestBody Expense expense,
            Authentication authentication) {
        return ResponseEntity.ok(
                expenseService.updateExpense(id, expense, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable Integer id,
            Authentication authentication) {
        expenseService.deleteExpense(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }

    // ═══════════════════════════════════════════════════════════
    // EXPENSE CATEGORY ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<ExpenseCategory> createCategory(
            @RequestBody ExpenseCategory category,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createCategory(category, JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<Page<ExpenseCategory>> listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return ResponseEntity.ok(
                expenseService.listCategories(JwtContextHolder.getHospitalId(), pageable));
    }

    @GetMapping("/categories/all")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<List<ExpenseCategory>> listAllCategories(Authentication authentication) {
        return ResponseEntity.ok(
                expenseService.listAllCategories(JwtContextHolder.getHospitalId()));
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<ExpenseCategory> getCategoryById(
            @PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(
                expenseService.getCategoryById(id, JwtContextHolder.getHospitalId()));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('Accountant', 'admin', 'superadmin')")
    public ResponseEntity<ExpenseCategory> updateCategory(
            @PathVariable Integer id,
            @RequestBody ExpenseCategory category,
            Authentication authentication) {
        return ResponseEntity.ok(
                expenseService.updateCategory(id, category, JwtContextHolder.getHospitalId()));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('admin', 'superadmin')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Integer id,
            Authentication authentication) {
        expenseService.deleteCategory(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
