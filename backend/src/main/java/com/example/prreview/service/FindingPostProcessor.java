package com.example.prreview.service;

import com.example.prreview.dto.diff.ChangedFileContext;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.dto.review.RiskItemDTO;
import com.example.prreview.enums.FindingCategory;
import com.example.prreview.enums.FindingKind;
import com.example.prreview.enums.FindingLevel;
import com.example.prreview.enums.PrType;
import com.example.prreview.util.GitHubDiffUrlBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FindingPostProcessor {

    public AiReviewReportDTO postProcess(ReviewContext reviewContext, AiReviewReportDTO report) {
        List<RiskItemDTO> normalizedFindings = deduplicate(processFindings(reviewContext, report.getFindings()));
        report.setFindings(normalizedFindings);
        report.setOverallConclusion(adjustConclusion(reviewContext.prType(), report.getOverallConclusion(), normalizedFindings.size()));
        return report;
    }

    private List<RiskItemDTO> processFindings(ReviewContext reviewContext, List<RiskItemDTO> findings) {
        if (findings == null || findings.isEmpty()) {
            return List.of();
        }

        Map<String, ChangedFileContext> fileContextMap = new LinkedHashMap<>();
        for (ChangedFileContext file : reviewContext.files()) {
            fileContextMap.put(normalizePath(file.filePath()), file);
        }

        List<RiskItemDTO> processed = new ArrayList<>();
        for (RiskItemDTO finding : findings) {
            if (finding == null) {
                continue;
            }
            ChangedFileContext fileContext = fileContextMap.get(normalizePath(finding.getFilePath()));
            String fileCategory = fileContext == null ? null : fileContext.fileCategory();
            FindingCategory category = resolveCategory(finding.getFindingCategory(), fileCategory);
            FindingLevel level = resolveLevel(finding.getFindingLevel(), category, fileCategory, finding);
            FindingKind kind = resolveKind(finding.getFindingKind(), level);

            finding.setFindingCategory(category.name());
            finding.setFindingLevel(level.name());
            finding.setFindingKind(kind.name());
            finding.setTitle(defaultTitle(finding.getTitle(), category, finding.getDescription()));
            finding.setDiffUrl(StringUtils.hasText(finding.getDiffUrl())
                    ? finding.getDiffUrl()
                    : GitHubDiffUrlBuilder.build(reviewContext.prUrl(), finding.getFilePath(), finding.getLineNumber()));
            processed.add(finding);
        }
        return processed;
    }

    private List<RiskItemDTO> deduplicate(List<RiskItemDTO> findings) {
        Map<String, RiskItemDTO> unique = new LinkedHashMap<>();
        List<RiskItemDTO> sorted = findings.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RiskItemDTO::getFilePath, Comparator.nullsLast(String::compareTo))
                        .thenComparing(RiskItemDTO::getLineNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(RiskItemDTO::getFindingCategory, Comparator.nullsLast(String::compareTo))
                        .thenComparing(RiskItemDTO::getTitle, Comparator.nullsLast(String::compareTo)))
                .toList();

        for (RiskItemDTO finding : sorted) {
            String key = String.join("|",
                    nullToEmpty(finding.getFilePath()),
                    String.valueOf(finding.getLineNumber()),
                    nullToEmpty(finding.getFindingCategory()));
            unique.putIfAbsent(key, finding);
        }

        return new ArrayList<>(unique.values());
    }

    private FindingCategory resolveCategory(String rawCategory, String fileCategory) {
        if (StringUtils.hasText(rawCategory)) {
            try {
                return FindingCategory.valueOf(rawCategory.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fallback below.
            }
        }

        if ("DOCUMENTATION".equalsIgnoreCase(fileCategory)) {
            return FindingCategory.DOCUMENTATION_FORMAT;
        }
        if ("TEST".equalsIgnoreCase(fileCategory)) {
            return FindingCategory.TEST_GAP;
        }
        if ("CONFIG".equalsIgnoreCase(fileCategory)) {
            return FindingCategory.CONFIG_COMPATIBILITY;
        }
        return FindingCategory.OTHER;
    }

    private FindingLevel resolveLevel(
            String rawLevel,
            FindingCategory category,
            String fileCategory,
            RiskItemDTO finding
    ) {
        FindingLevel level = parseLevel(rawLevel);

        if (isDocumentationLike(category, fileCategory)) {
            if (level == FindingLevel.HIGH || level == FindingLevel.MEDIUM) {
                level = FindingLevel.LOW;
            }
            if (lacksEvidence(finding)) {
                level = FindingLevel.ADVISORY;
            }
            return level;
        }

        if (level == FindingLevel.HIGH && lacksEvidence(finding)) {
            return FindingLevel.MEDIUM;
        }

        if ((level == FindingLevel.HIGH || level == FindingLevel.MEDIUM) && finding.getLineNumber() == null
                && !StringUtils.hasText(finding.getCodeSnippet())) {
            return FindingLevel.ADVISORY;
        }

        return level;
    }

    private boolean isDocumentationLike(FindingCategory category, String fileCategory) {
        return "DOCUMENTATION".equalsIgnoreCase(fileCategory)
                || category == FindingCategory.DOCUMENTATION_FORMAT
                || category == FindingCategory.COMMAND_EXECUTABILITY
                || category == FindingCategory.PATH_COMPATIBILITY
                || category == FindingCategory.MAINTAINABILITY;
    }

    private boolean lacksEvidence(RiskItemDTO finding) {
        return finding.getLineNumber() == null
                && !StringUtils.hasText(finding.getCodeSnippet())
                && !StringUtils.hasText(finding.getBeforeExample())
                && !StringUtils.hasText(finding.getAfterExample())
                && !StringUtils.hasText(finding.getSuggestedPatch());
    }

    private FindingKind resolveKind(String rawKind, FindingLevel level) {
        if (StringUtils.hasText(rawKind)) {
            try {
                return FindingKind.valueOf(rawKind.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fallback below.
            }
        }
        return level == FindingLevel.ADVISORY ? FindingKind.ADVISORY : FindingKind.RISK;
    }

    private FindingLevel parseLevel(String rawLevel) {
        if (!StringUtils.hasText(rawLevel)) {
            return FindingLevel.ADVISORY;
        }
        try {
            return FindingLevel.valueOf(rawLevel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FindingLevel.ADVISORY;
        }
    }

    private String defaultTitle(String rawTitle, FindingCategory category, String description) {
        if (StringUtils.hasText(rawTitle)) {
            return rawTitle.trim();
        }
        if (StringUtils.hasText(description)) {
            String compact = description.replaceAll("\\s+", " ").trim();
            return compact.length() <= 48 ? compact : compact.substring(0, 48) + "...";
        }
        return switch (category) {
            case DOCUMENTATION_FORMAT -> "文档格式问题";
            case COMMAND_EXECUTABILITY -> "命令可执行性问题";
            case PATH_COMPATIBILITY -> "路径兼容性问题";
            case TEST_GAP -> "测试缺口";
            case CODE_LOGIC -> "代码逻辑风险";
            case SECURITY -> "安全风险";
            case EXCEPTION_HANDLING -> "异常处理问题";
            case PERFORMANCE -> "性能问题";
            case MAINTAINABILITY -> "可维护性问题";
            case CONFIG_COMPATIBILITY -> "配置兼容性问题";
            case DEPENDENCY_CHANGE -> "依赖变更问题";
            case CICD_CHANGE -> "CI/CD 变更问题";
            case OTHER -> "Review 发现项";
        };
    }

    private String adjustConclusion(PrType prType, String overallConclusion, int findingCount) {
        if (prType == null) {
            return overallConclusion;
        }
        if (StringUtils.hasText(overallConclusion)) {
            return overallConclusion;
        }
        return switch (prType) {
            case DOCUMENTATION -> findingCount == 0
                    ? "本次 PR 主要是文档变更，未发现明确文档问题，建议人工检查渲染效果与命令可执行性。"
                    : "本次 PR 主要是文档变更，建议优先处理文档质量、命令可执行性和路径表达相关问题。";
            case CODE -> findingCount == 0
                    ? "本次实现变更未发现明确高风险问题，建议结合业务场景完成常规人工复核。"
                    : "本次实现变更存在需要关注的发现项，建议优先处理实现风险和测试覆盖问题。";
            case CONFIG, TEST, DEPENDENCY, CICD, MIXED -> findingCount == 0
                    ? "本次 PR 未发现明确高信号问题，建议结合实际环境完成针对性验证。"
                    : "本次 PR 存在需要复核的发现项，建议结合对应变更类型完成验证和调整。";
        };
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.replace("\\", "/").trim().toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
