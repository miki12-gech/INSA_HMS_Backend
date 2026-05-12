package com.insa.hospital.service;

import com.insa.hospital.entity.Expense;
import com.insa.hospital.entity.ExpenseCategory;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.ExpenseCategoryRepository;
import com.insa.hospital.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Expense Service — Business Logic for Expense & ExpenseCategory modules.
 */
@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpenseCategoryRepository categoryRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
    }

    // ─── Expenses ─────────────────────────────────────────────────────────────

    public Expense createExpense(Expense expense, String hospitalId, String userId) {
        expense.setHospitalId(hospitalId);
        if (!StringUtils.hasText(expense.getUser())) {
            expense.setUser(userId);
        }
        if (!StringUtils.hasText(expense.getDate())) {
            expense.setDate(String.valueOf(System.currentTimeMillis() / 1000L));
        }
        return expenseRepository.save(expense);
    }

    @Transactional(readOnly = true)
    public Page<Expense> listExpenses(String hospitalId, Pageable pageable) {
        return expenseRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Expense> listExpensesByCategory(String category, String hospitalId, Pageable pageable) {
        return expenseRepository.findByCategoryAndHospitalId(category, hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public Expense getExpenseById(Integer id, String hospitalId) {
        return expenseRepository.findById(id)
                .filter(e -> hospitalId.equals(e.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
    }

    public Expense updateExpense(Integer id, Expense updated, String hospitalId) {
        Expense existing = getExpenseById(id, hospitalId);
        existing.setCategory(updated.getCategory());
        existing.setDate(updated.getDate());
        existing.setNote(updated.getNote());
        existing.setAmount(updated.getAmount());
        return expenseRepository.save(existing);
    }

    public void deleteExpense(Integer id, String hospitalId) {
        expenseRepository.delete(getExpenseById(id, hospitalId));
    }

    // ─── Expense Categories ───────────────────────────────────────────────────

    public ExpenseCategory createCategory(ExpenseCategory category, String hospitalId) {
        category.setHospitalId(hospitalId);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseCategory> listCategories(String hospitalId, Pageable pageable) {
        return categoryRepository.findByHospitalId(hospitalId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> listAllCategories(String hospitalId) {
        return categoryRepository.findByHospitalId(hospitalId);
    }

    @Transactional(readOnly = true)
    public ExpenseCategory getCategoryById(Integer id, String hospitalId) {
        return categoryRepository.findById(id)
                .filter(c -> hospitalId.equals(c.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", "id", id));
    }

    public ExpenseCategory updateCategory(Integer id, ExpenseCategory updated, String hospitalId) {
        ExpenseCategory existing = getCategoryById(id, hospitalId);
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setX(updated.getX());
        existing.setY(updated.getY());
        return categoryRepository.save(existing);
    }

    public void deleteCategory(Integer id, String hospitalId) {
        categoryRepository.delete(getCategoryById(id, hospitalId));
    }
}
