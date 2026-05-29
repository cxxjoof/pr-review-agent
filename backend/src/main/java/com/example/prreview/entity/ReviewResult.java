package com.example.prreview.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "review_result",
        indexes = {
            @Index(name = "uk_review_result_task_id", columnList = "task_id", unique = true)
        }
)
public class ReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "task_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_review_result_task")
    )
    private ReviewTask task;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "changed_modules", columnDefinition = "json")
    private String changedModules;

    @Column(name = "review_suggestions", columnDefinition = "json")
    private String reviewSuggestions;

    @Column(name = "test_suggestions", columnDefinition = "json")
    private String testSuggestions;

    @Column(name = "overall_conclusion", columnDefinition = "TEXT")
    private String overallConclusion;

    @Lob
    @Column(name = "raw_ai_response")
    private String rawAiResponse;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getChangedModules() {
        return changedModules;
    }

    public void setChangedModules(String changedModules) {
        this.changedModules = changedModules;
    }

    public String getReviewSuggestions() {
        return reviewSuggestions;
    }

    public void setReviewSuggestions(String reviewSuggestions) {
        this.reviewSuggestions = reviewSuggestions;
    }

    public String getTestSuggestions() {
        return testSuggestions;
    }

    public void setTestSuggestions(String testSuggestions) {
        this.testSuggestions = testSuggestions;
    }

    public String getOverallConclusion() {
        return overallConclusion;
    }

    public void setOverallConclusion(String overallConclusion) {
        this.overallConclusion = overallConclusion;
    }

    public String getRawAiResponse() {
        return rawAiResponse;
    }

    public void setRawAiResponse(String rawAiResponse) {
        this.rawAiResponse = rawAiResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
