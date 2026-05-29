package com.example.prreview.dto.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReviewReportDTO {

    private String summary;

    private List<String> changedModules = new ArrayList<>();

    private List<RiskItemDTO> riskItems = new ArrayList<>();

    private List<String> reviewSuggestions = new ArrayList<>();

    private List<String> testSuggestions = new ArrayList<>();

    private String overallConclusion;

    public AiReviewReportDTO normalize(List<String> defaultChangedModules) {
        summary = cleanText(summary);
        overallConclusion = cleanText(overallConclusion);
        changedModules = normalizeStrings(changedModules);
        reviewSuggestions = normalizeStrings(reviewSuggestions);
        testSuggestions = normalizeStrings(testSuggestions);
        riskItems = riskItems == null
                ? new ArrayList<>()
                : riskItems.stream()
                        .filter(Objects::nonNull)
                        .peek(RiskItemDTO::normalize)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (changedModules.isEmpty() && defaultChangedModules != null) {
            changedModules = normalizeStrings(defaultChangedModules);
        }

        return this;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getChangedModules() {
        return changedModules;
    }

    public void setChangedModules(List<String> changedModules) {
        this.changedModules = changedModules;
    }

    public List<RiskItemDTO> getRiskItems() {
        return riskItems;
    }

    public void setRiskItems(List<RiskItemDTO> riskItems) {
        this.riskItems = riskItems;
    }

    public List<String> getReviewSuggestions() {
        return reviewSuggestions;
    }

    public void setReviewSuggestions(List<String> reviewSuggestions) {
        this.reviewSuggestions = reviewSuggestions;
    }

    public List<String> getTestSuggestions() {
        return testSuggestions;
    }

    public void setTestSuggestions(List<String> testSuggestions) {
        this.testSuggestions = testSuggestions;
    }

    public String getOverallConclusion() {
        return overallConclusion;
    }

    public void setOverallConclusion(String overallConclusion) {
        this.overallConclusion = overallConclusion;
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .map(this::cleanText)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }
}
