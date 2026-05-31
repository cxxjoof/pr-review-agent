package com.example.prreview.entity;

import com.example.prreview.enums.FeedbackType;
import com.example.prreview.enums.FindingCategory;
import com.example.prreview.enums.FindingKind;
import com.example.prreview.enums.FindingLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "risk_item",
        indexes = {
            @Index(name = "idx_risk_item_task_id", columnList = "task_id"),
            @Index(name = "idx_finding_level", columnList = "finding_level"),
            @Index(name = "idx_file_path", columnList = "file_path")
        }
)
public class RiskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "task_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_risk_item_task")
    )
    private ReviewTask task;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_level", length = 30)
    private FindingLevel findingLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_kind", length = 30)
    private FindingKind findingKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_category", length = 100)
    private FindingCategory findingCategory;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "before_example", columnDefinition = "TEXT")
    private String beforeExample;

    @Column(name = "after_example", columnDefinition = "TEXT")
    private String afterExample;

    @Column(name = "suggested_patch", columnDefinition = "TEXT")
    private String suggestedPatch;

    @Column(name = "diff_url", length = 1000)
    private String diffUrl;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", length = 30)
    private FeedbackType feedbackStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReviewTask getTask() {
        return task;
    }

    public void setTask(ReviewTask task) {
        this.task = task;
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

    public FindingLevel getFindingLevel() {
        return findingLevel;
    }

    public void setFindingLevel(FindingLevel findingLevel) {
        this.findingLevel = findingLevel;
    }

    public FindingKind getFindingKind() {
        return findingKind;
    }

    public void setFindingKind(FindingKind findingKind) {
        this.findingKind = findingKind;
    }

    public FindingCategory getFindingCategory() {
        return findingCategory;
    }

    public void setFindingCategory(FindingCategory findingCategory) {
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

    public FeedbackType getFeedbackStatus() {
        return feedbackStatus;
    }

    public void setFeedbackStatus(FeedbackType feedbackStatus) {
        this.feedbackStatus = feedbackStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
