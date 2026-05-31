package com.example.prreview.dto.review;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskItemDTO {

    private String filePath;

    private Integer lineNumber;

    private String codeSnippet;

    private String findingLevel;

    private String findingKind;

    private String findingCategory;

    private String title;

    private String description;

    private String suggestion;

    private String beforeExample;

    private String afterExample;

    private String suggestedPatch;

    private String diffUrl;

    private BigDecimal confidence;

    public void normalize() {
        filePath = cleanText(filePath);
        codeSnippet = cleanText(codeSnippet);
        findingLevel = cleanText(findingLevel);
        findingKind = cleanText(findingKind);
        findingCategory = cleanText(findingCategory);
        title = cleanText(title);
        description = cleanText(description);
        suggestion = cleanText(suggestion);
        beforeExample = cleanText(beforeExample);
        afterExample = cleanText(afterExample);
        suggestedPatch = cleanText(suggestedPatch);
        diffUrl = cleanText(diffUrl);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }

    public String getFindingLevel() {
        return findingLevel;
    }

    public void setFindingLevel(String findingLevel) {
        this.findingLevel = findingLevel;
    }

    public String getFindingKind() {
        return findingKind;
    }

    public void setFindingKind(String findingKind) {
        this.findingKind = findingKind;
    }

    public String getFindingCategory() {
        return findingCategory;
    }

    public void setFindingCategory(String findingCategory) {
        this.findingCategory = findingCategory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getBeforeExample() {
        return beforeExample;
    }

    public void setBeforeExample(String beforeExample) {
        this.beforeExample = beforeExample;
    }

    public String getAfterExample() {
        return afterExample;
    }

    public void setAfterExample(String afterExample) {
        this.afterExample = afterExample;
    }

    public String getSuggestedPatch() {
        return suggestedPatch;
    }

    public void setSuggestedPatch(String suggestedPatch) {
        this.suggestedPatch = suggestedPatch;
    }

    public String getDiffUrl() {
        return diffUrl;
    }

    public void setDiffUrl(String diffUrl) {
        this.diffUrl = diffUrl;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }
}
