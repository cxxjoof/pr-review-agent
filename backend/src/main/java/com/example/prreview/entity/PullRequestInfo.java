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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pull_request_info",
        indexes = {
            @Index(name = "uk_pull_request_info_task_id", columnList = "task_id", unique = true)
        }
)
public class PullRequestInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "task_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pr_info_task")
    )
    private ReviewTask task;

    @Column(name = "github_pr_id")
    private Long githubPrId;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String author;

    @Column(name = "source_branch", length = 200)
    private String sourceBranch;

    @Column(name = "target_branch", length = 200)
    private String targetBranch;

    @Column(length = 30)
    private String state;

    @Column(name = "changed_files", nullable = false)
    private Integer changedFiles;

    @Column(nullable = false)
    private Integer additions;

    @Column(nullable = false)
    private Integer deletions;

    @Column(nullable = false)
    private Integer commits;

    @Column(name = "pr_url", length = 500)
    private String prUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (changedFiles == null) {
            changedFiles = 0;
        }
        if (additions == null) {
            additions = 0;
        }
        if (deletions == null) {
            deletions = 0;
        }
        if (commits == null) {
            commits = 0;
        }
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

    public Long getGithubPrId() {
        return githubPrId;
    }

    public void setGithubPrId(Long githubPrId) {
        this.githubPrId = githubPrId;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(Integer changedFiles) {
        this.changedFiles = changedFiles;
    }

    public Integer getAdditions() {
        return additions;
    }

    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public Integer getCommits() {
        return commits;
    }

    public void setCommits(Integer commits) {
        this.commits = commits;
    }

    public String getPrUrl() {
        return prUrl;
    }

    public void setPrUrl(String prUrl) {
        this.prUrl = prUrl;
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
