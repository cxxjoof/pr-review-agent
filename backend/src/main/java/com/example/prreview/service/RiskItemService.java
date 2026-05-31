package com.example.prreview.service;

import com.example.prreview.dto.review.RiskItemDTO;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.FeedbackType;
import com.example.prreview.enums.FindingCategory;
import com.example.prreview.enums.FindingKind;
import com.example.prreview.enums.FindingLevel;
import com.example.prreview.exception.BusinessException;
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

    public List<RiskItem> replaceRiskItems(ReviewTask task, List<RiskItemDTO> findings) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Persisted review task is required.");
        }

        riskItemRepository.deleteByTask_Id(task.getId());
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }

        List<RiskItem> entities = findings.stream()
                .map(finding -> toEntity(task, finding))
                .toList();
        return riskItemRepository.saveAll(entities);
    }

    public RiskItem findByTaskIdAndFindingId(Long taskId, Long findingId) {
        return riskItemRepository.findByIdAndTaskId(findingId, taskId)
                .orElseThrow(() -> BusinessException.notFound("Finding not found for the review task."));
    }

    public RiskItem save(RiskItem finding) {
        return riskItemRepository.save(finding);
    }

    private RiskItem toEntity(ReviewTask task, RiskItemDTO findingDto) {
        RiskItem finding = new RiskItem();
        finding.setTask(task);
        finding.setFilePath(StringUtils.hasText(findingDto.getFilePath()) ? findingDto.getFilePath().trim() : "unknown");
        finding.setLineNumber(findingDto.getLineNumber());
        finding.setCodeSnippet(trimToNull(findingDto.getCodeSnippet()));
        finding.setFindingLevel(resolveFindingLevel(findingDto.getFindingLevel()));
        finding.setFindingKind(resolveFindingKind(findingDto.getFindingKind()));
        finding.setFindingCategory(resolveFindingCategory(findingDto.getFindingCategory()));
        finding.setTitle(trimToNull(findingDto.getTitle()));
        finding.setDescription(trimToNull(findingDto.getDescription()));
        finding.setSuggestion(trimToNull(findingDto.getSuggestion()));
        finding.setBeforeExample(trimToNull(findingDto.getBeforeExample()));
        finding.setAfterExample(trimToNull(findingDto.getAfterExample()));
        finding.setSuggestedPatch(trimToNull(findingDto.getSuggestedPatch()));
        finding.setDiffUrl(trimToNull(findingDto.getDiffUrl()));
        finding.setConfidence(normalizeConfidence(findingDto.getConfidence()));
        finding.setFeedbackStatus((FeedbackType) null);
        return finding;
    }

    private FindingLevel resolveFindingLevel(String findingLevel) {
        if (!StringUtils.hasText(findingLevel)) {
            return FindingLevel.ADVISORY;
        }

        try {
            return FindingLevel.valueOf(findingLevel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FindingLevel.ADVISORY;
        }
    }

    private FindingKind resolveFindingKind(String findingKind) {
        if (!StringUtils.hasText(findingKind)) {
            return FindingKind.ADVISORY;
        }

        try {
            return FindingKind.valueOf(findingKind.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FindingKind.ADVISORY;
        }
    }

    private FindingCategory resolveFindingCategory(String findingCategory) {
        if (!StringUtils.hasText(findingCategory)) {
            return FindingCategory.OTHER;
        }

        try {
            return FindingCategory.valueOf(findingCategory.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FindingCategory.OTHER;
        }
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        if (confidence == null) {
            return null;
        }

        return confidence.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
