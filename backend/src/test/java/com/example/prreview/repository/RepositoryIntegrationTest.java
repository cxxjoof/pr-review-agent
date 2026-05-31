package com.example.prreview.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.entity.ModelCallLog;
import com.example.prreview.entity.PullRequestInfo;
import com.example.prreview.entity.ReviewFeedback;
import com.example.prreview.entity.ReviewResult;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.FeedbackType;
import com.example.prreview.enums.FindingCategory;
import com.example.prreview.enums.FindingKind;
import com.example.prreview.enums.FindingLevel;
import com.example.prreview.enums.PrType;
import com.example.prreview.enums.TaskStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class RepositoryIntegrationTest {

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Autowired
    private PullRequestInfoRepository pullRequestInfoRepository;

    @Autowired
    private ReviewResultRepository reviewResultRepository;

    @Autowired
    private RiskItemRepository riskItemRepository;

    @Autowired
    private ModelCallLogRepository modelCallLogRepository;

    @Autowired
    private ReviewFeedbackRepository reviewFeedbackRepository;

    @Test
    void shouldPersistTaskAndRelatedRecords() {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl("https://github.com/example/repo");
        task.setRepoOwner("example");
        task.setRepoName("repo");
        task.setPrNumber(12);
        task.setPrType(PrType.CODE);
        task.setStatus(TaskStatus.RUNNING);
        ReviewTask savedTask = reviewTaskRepository.save(task);

        PullRequestInfo prInfo = new PullRequestInfo();
        prInfo.setTask(savedTask);
        prInfo.setGithubPrId(1001L);
        prInfo.setTitle("feat: add repository layer");
        prInfo.setDescription("Persist task and review metadata");
        prInfo.setAuthor("octocat");
        prInfo.setSourceBranch("feature/repository-layer");
        prInfo.setTargetBranch("main");
        prInfo.setState("open");
        prInfo.setChangedFiles(5);
        prInfo.setAdditions(120);
        prInfo.setDeletions(18);
        prInfo.setCommits(3);
        prInfo.setPrUrl("https://github.com/example/repo/pull/12");
        pullRequestInfoRepository.save(prInfo);

        ReviewResult reviewResult = new ReviewResult();
        reviewResult.setTask(savedTask);
        reviewResult.setSummary("Adds persistence support for review tasks.");
        reviewResult.setChangedModules("[\"backend\"]");
        reviewResult.setReviewSuggestions("[\"Add repository tests\"]");
        reviewResult.setTestSuggestions("[\"Verify task status transitions\"]");
        reviewResult.setOverallConclusion("Looks good with minor follow-up suggestions.");
        reviewResult.setRawAiResponse("{\"summary\":\"ok\"}");
        reviewResultRepository.save(reviewResult);

        RiskItem riskItem = new RiskItem();
        riskItem.setTask(savedTask);
        riskItem.setFilePath("backend/src/main/java/com/example/prreview/entity/ReviewTask.java");
        riskItem.setLineNumber(42);
        riskItem.setCodeSnippet("task.setStatus(TaskStatus.RUNNING);");
        riskItem.setFindingLevel(FindingLevel.MEDIUM);
        riskItem.setFindingKind(FindingKind.RISK);
        riskItem.setFindingCategory(FindingCategory.TEST_GAP);
        riskItem.setTitle("补充状态迁移测试");
        riskItem.setDescription("Task status changes should be covered by tests.");
        riskItem.setSuggestion("Add repository and service-level tests for transitions.");
        riskItem.setConfidence(new BigDecimal("0.86"));
        RiskItem savedFinding = riskItemRepository.save(riskItem);

        ReviewFeedback feedback = new ReviewFeedback();
        feedback.setTask(savedTask);
        feedback.setFinding(savedFinding);
        feedback.setFeedbackType(FeedbackType.USEFUL);
        feedback.setComment("This is actionable.");
        reviewFeedbackRepository.save(feedback);

        ModelCallLog modelCallLog = new ModelCallLog();
        modelCallLog.setTask(savedTask);
        modelCallLog.setModelName("gpt-4.1");
        modelCallLog.setCallType("SUMMARY");
        modelCallLog.setPromptTokens(128);
        modelCallLog.setCompletionTokens(64);
        modelCallLog.setTotalTokens(192);
        modelCallLog.setSuccess(Boolean.TRUE);
        modelCallLog.setLatencyMs(820);
        modelCallLogRepository.save(modelCallLog);

        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getRiskCount()).isZero();
        assertThat(savedTask.getCreatedAt()).isNotNull();
        assertThat(savedTask.getUpdatedAt()).isNotNull();

        assertThat(pullRequestInfoRepository.findByTaskId(savedTask.getId()))
                .get()
                .extracting(PullRequestInfo::getTitle)
                .isEqualTo("feat: add repository layer");

        assertThat(reviewResultRepository.findByTaskId(savedTask.getId()))
                .get()
                .extracting(ReviewResult::getSummary)
                .isEqualTo("Adds persistence support for review tasks.");

        List<RiskItem> mediumFindings = riskItemRepository.findByTaskIdAndFindingLevel(savedTask.getId(), FindingLevel.MEDIUM);
        assertThat(mediumFindings).hasSize(1);
        assertThat(mediumFindings.get(0).getFindingCategory()).isEqualTo(FindingCategory.TEST_GAP);

        List<ReviewFeedback> feedbackLogs = reviewFeedbackRepository.findByTaskIdOrderByCreatedAtDesc(savedTask.getId());
        assertThat(feedbackLogs).hasSize(1);
        assertThat(feedbackLogs.get(0).getFeedbackType()).isEqualTo(FeedbackType.USEFUL);

        List<ModelCallLog> logs = modelCallLogRepository.findByTaskId(savedTask.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getSuccess()).isTrue();

        assertThat(reviewTaskRepository.findByRepoOwnerAndRepoNameAndPrNumber("example", "repo", 12))
                .hasSize(1);
        assertThat(reviewTaskRepository.findByStatus(TaskStatus.RUNNING))
                .hasSize(1);
    }
}
