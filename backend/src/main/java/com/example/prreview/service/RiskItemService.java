package com.example.prreview.service;

import com.example.prreview.dto.review.RiskItemDTO;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.RiskLevel;
import com.example.prreview.enums.RiskType;
import com.example.prreview.repository.RiskItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RiskItemService {

    private final RiskItemRepository riskItemRepository;

    public RiskItemService(RiskItemRepository riskItemRepository) {
        this.riskItemRepository = riskItemRepository;
    }

    public List<RiskItem> replaceRiskItems(ReviewTask task, List<RiskItemDTO> riskItems) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Persisted review task is required.");
        }

        riskItemRepository.deleteByTask_Id(task.getId());
        if (riskItems == null || riskItems.isEmpty()) {
            return List.of();
        }

        List<RiskItem> entities = riskItems.stream()
                .map(riskItem -> toEntity(task, riskItem))
                .toList();
        return riskItemRepository.saveAll(entities);
    }

    private RiskItem toEntity(ReviewTask task, RiskItemDTO riskItemDto) {
        RiskItem riskItem = new RiskItem();
        riskItem.setTask(task);
        riskItem.setFilePath(StringUtils.hasText(riskItemDto.getFilePath()) ? riskItemDto.getFilePath().trim() : "unknown");
        riskItem.setLineNumber(riskItemDto.getLineNumber());
        riskItem.setCodeSnippet(trimToNull(riskItemDto.getCodeSnippet()));
        riskItem.setRiskLevel(resolveRiskLevel(riskItemDto.getRiskLevel()));
        riskItem.setRiskType(resolveRiskType(riskItemDto.getRiskType()));
        riskItem.setDescription(trimToNull(riskItemDto.getDescription()));
        riskItem.setSuggestion(trimToNull(riskItemDto.getSuggestion()));
        riskItem.setConfidence(normalizeConfidence(riskItemDto.getConfidence()));
        return riskItem;
    }

    private RiskLevel resolveRiskLevel(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) {
            return RiskLevel.MEDIUM;
        }

        try {
            return RiskLevel.valueOf(riskLevel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return RiskLevel.LOW;
        }
    }

    private RiskType resolveRiskType(String riskType) {
        if (!StringUtils.hasText(riskType)) {
            return RiskType.OTHER;
        }

        try {
            return RiskType.valueOf(riskType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return RiskType.OTHER;
        }
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return null;
        }

        return confidence.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
