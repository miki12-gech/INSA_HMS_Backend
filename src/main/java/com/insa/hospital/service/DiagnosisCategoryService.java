package com.insa.hospital.service;

import com.insa.hospital.dto.DiagnosisCategoryDto;
import com.insa.hospital.dto.DiagnosisCategoryRequestDto;
import com.insa.hospital.entity.DiagnosisCategory;
import com.insa.hospital.repository.DiagnosisCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosisCategoryService {

    private final DiagnosisCategoryRepository diagnosisCategoryRepository;

    public List<DiagnosisCategoryDto> list(String query) {
        List<DiagnosisCategory> items = (query == null || query.trim().isEmpty())
                ? diagnosisCategoryRepository.findAll()
                : diagnosisCategoryRepository.findByCategoryContainingIgnoreCase(query.trim());

        return items.stream()
                .sorted(Comparator.comparing(item -> normalizeText(item.getCategory()), String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public DiagnosisCategoryDto create(DiagnosisCategoryRequestDto requestDto) {
        String category = normalizeText(requestDto.getCategory());
        String metadata = toSeoMetadata(category, requestDto.getDescription());

        DiagnosisCategory existing = diagnosisCategoryRepository.findByCategoryIgnoreCase(category).orElse(null);
        if (existing != null) {
            return toDto(existing);
        }

        DiagnosisCategory entity = new DiagnosisCategory();
        entity.setCategory(category);
        entity.setDescription(metadata);
        return toDto(diagnosisCategoryRepository.save(entity));
    }

    private DiagnosisCategoryDto toDto(DiagnosisCategory e) {
        DiagnosisCategoryDto dto = new DiagnosisCategoryDto();
        dto.setId(e.getId());
        dto.setCategory(e.getCategory());
        dto.setDescription(e.getDescription());
        return dto;
    }

    private String toSeoMetadata(String category, String rawDescription) {
        String description = normalizePlainText(rawDescription);
        String metadata = description;

        if (!description.toLowerCase().contains(category.toLowerCase())) {
            metadata = category + ": " + description;
        }

        if (metadata.length() > 100) {
            metadata = metadata.substring(0, 100).trim();
        }

        return metadata;
    }

    private String normalizePlainText(String value) {
        return normalizeText(value.replaceAll("<[^>]+>", " "));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
