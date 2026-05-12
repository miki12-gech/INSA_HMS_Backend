package com.insa.hospital.service;

import com.insa.hospital.dto.PaymentCategoryUpsertDto;
import com.insa.hospital.entity.PaymentCategory;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.PaymentCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BillingCatalogService {

    private final PaymentCategoryRepository paymentCategoryRepository;

    @Transactional(readOnly = true)
    public List<PaymentCategory> listCatalog(String hospitalId, boolean activeOnly) {
        return paymentCategoryRepository.findByHospitalId(hospitalId).stream()
                .filter(category -> !activeOnly || !Boolean.FALSE.equals(category.getActive()))
                .sorted(Comparator.comparing(category -> category.getCategory() == null ? "" : category.getCategory(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public PaymentCategory createCategory(PaymentCategoryUpsertDto dto, String hospitalId) {
        PaymentCategory category = new PaymentCategory();
        apply(category, dto, hospitalId);
        return paymentCategoryRepository.save(category);
    }

    public PaymentCategory updateCategory(Integer id, PaymentCategoryUpsertDto dto, String hospitalId) {
        PaymentCategory category = getScopedCategory(id, hospitalId);
        apply(category, dto, hospitalId);
        return paymentCategoryRepository.save(category);
    }

    public PaymentCategory updateCategoryStatus(Integer id, boolean active, String hospitalId) {
        PaymentCategory category = getScopedCategory(id, hospitalId);
        category.setActive(active);
        return paymentCategoryRepository.save(category);
    }

    public void deleteCategory(Integer id, String hospitalId) {
        PaymentCategory category = getScopedCategory(id, hospitalId);
        paymentCategoryRepository.delete(category);
    }

    private PaymentCategory getScopedCategory(Integer id, String hospitalId) {
        return paymentCategoryRepository.findById(id)
                .filter(category -> hospitalId.equals(category.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("PaymentCategory", "id", id));
    }

    private void apply(PaymentCategory category, PaymentCategoryUpsertDto dto, String hospitalId) {
        category.setHospitalId(hospitalId);
        category.setCategory(dto.category().trim());
        category.setDescription(trimToNull(dto.description()));
        category.setCPrice(StringUtils.hasText(dto.cPrice()) ? dto.cPrice().trim() : "0");
        category.setType(StringUtils.hasText(dto.type()) ? dto.type().trim() : "others");
        category.setDCommission(dto.dCommission() != null ? dto.dCommission() : 0);
        category.setHCommission(dto.hCommission() != null ? dto.hCommission() : 0);
        category.setServiceGroup(trimToNull(dto.serviceGroup()));
        category.setServiceRole(trimToNull(dto.serviceRole()));
        category.setRevenueTarget(trimToNull(dto.revenueTarget()));
        category.setActive(dto.active() == null || dto.active());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
