package com.example.prreview.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prreview.client.GitHubClient;
import com.example.prreview.client.ModelClient;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.dto.model.ChatMessage;
import com.example.prreview.entity.PullRequestInfo;
import com.example.prreview.entity.ReviewResult;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.ModelCallLogRepository;
import com.example.prreview.repository.PullRequestInfoRepository;
import com.example.prreview.repository.ReviewResultRepository;
import com.example.prreview.repository.ReviewTaskRepository;
import com.example.prreview.repository.RiskItemRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @MockBean
    private GitHubClient gitHubClient;

    @MockBean
    private ModelClient modelClient;

    @BeforeEach
    void setUp() {
        modelCallLogRepository.deleteAll();
        riskItemRepository.deleteAll();
        reviewResultRepository.deleteAll();
        pullRequestInfoRepository.deleteAll();
        reviewTaskRepository.deleteAll();
    }

    @Test
    void shouldCreateReviewTaskAndReturnUnifiedResponse() throws Exception {
        mockReviewFlow(
                11,
                "feat: add review task api",
                """
                {
                  "summary": "This PR adds the review task API.",
                  "changedModules": ["backend/controller", "backend/service"],
                  "riskItems": [
                    {
                      "filePath": "backend/src/main/java/com/example/prreview/service/ReviewTaskService.java",
                      "lineNumber": 42,
                      "riskLevel": "MEDIUM",
                      "riskType": "EXCEPTION_HANDLING",
                      "description": "Exception flow should be verified.",
                      "suggestion": "Add explicit error handling for external calls.",
                      "confidence": 0.88
                    }
                  ],
                  "reviewSuggestions": ["Verify failure states."],
                  "testSuggestions": ["Add endpoint integration tests."],
                  "overallConclusion": "The change is reasonable with a moderate integration risk."
                }
                """
        );

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repoUrl": "https://github.com/openai/pr-review-agent",
                                  "prNumber": 11
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.taskId").isNumber())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.riskCount").value(1))
                .andExpect(jsonPath("$.data.summary").value("This PR adds the review task API."))
                .andExpect(jsonPath("$.data.overallConclusion")
                        .value("The change is reasonable with a moderate integration risk."));

        List<ReviewTask> tasks = reviewTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(tasks.get(0).getRiskCount()).isEqualTo(1);

        PullRequestInfo pullRequestInfo = pullRequestInfoRepository.findByTaskId(tasks.get(0).getId()).orElseThrow();
        assertThat(pullRequestInfo.getTitle()).isEqualTo("feat: add review task api");

        ReviewResult reviewResult = reviewResultRepository.findByTaskId(tasks.get(0).getId()).orElseThrow();
        assertThat(reviewResult.getSummary()).isEqualTo("This PR adds the review task API.");

        List<RiskItem> riskItems = riskItemRepository.findByTaskId(tasks.get(0).getId());
        assertThat(riskItems).hasSize(1);
        assertThat(riskItems.get(0).getRiskLevel().name()).isEqualTo("MEDIUM");
    }

    @Test
    void shouldReturnReviewTaskDetail() throws Exception {
        mockReviewFlow(
                12,
                "feat: expose review detail api",
                """
                {
                  "summary": "This PR exposes review detail APIs.",
                  "changedModules": ["backend/controller"],
                  "riskItems": [],
                  "reviewSuggestions": ["Confirm response fields stay backward compatible."],
                  "testSuggestions": ["Add detail endpoint coverage."],
                  "overallConclusion": "Low risk change."
                }
                """
        );

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repoUrl": "https://github.com/openai/pr-review-agent",
                                  "prNumber": 12
                                }
                                """))
                .andExpect(status().isOk());

        ReviewTask task = reviewTaskRepository.findAll().getFirst();

        mockMvc.perform(get("/api/reviews/{id}", task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(task.getId()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.pullRequest.repoOwner").value("openai"))
                .andExpect(jsonPath("$.data.pullRequest.repoName").value("pr-review-agent"))
                .andExpect(jsonPath("$.data.pullRequest.title").value("feat: expose review detail api"))
                .andExpect(jsonPath("$.data.reviewResult.summary").value("This PR exposes review detail APIs."))
                .andExpect(jsonPath("$.data.reviewResult.changedModules[0]").value("backend/controller"))
                .andExpect(jsonPath("$.data.reviewResult.reviewSuggestions[0]")
                        .value("Confirm response fields stay backward compatible."))
                .andExpect(jsonPath("$.data.riskItems").isArray());
    }

    @Test
    void shouldListReviewTasks() throws Exception {
        ReviewTask olderTask = saveTask(
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                1,
                TaskStatus.SUCCESS,
                2,
                null
        );
        saveReviewResult(olderTask, "Older review summary", "Earlier conclusion");

        ReviewTask newerTask = saveTask(
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                2,
                TaskStatus.FAILED,
                0,
                "Model call failed"
        );

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(newerTask.getId()))
                .andExpect(jsonPath("$.data[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data[0].errorMessage").value("Model call failed"))
                .andExpect(jsonPath("$.data[1].taskId").value(olderTask.getId()))
                .andExpect(jsonPath("$.data[1].summary").value("Older review summary"))
                .andExpect(jsonPath("$.data[1].overallConclusion").value("Earlier conclusion"));
    }

    private void mockReviewFlow(int prNumber, String title, String aiResponseJson) {
        GitHubClient.GitHubRepository repository = new GitHubClient.GitHubRepository("openai", "pr-review-agent");
        GitHubPullRequestDTO pullRequest = new GitHubPullRequestDTO(
                null,
                1000L + prNumber,
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                prNumber,
                title,
                "Review task api implementation",
                "octocat",
                "feature/review-api",
                "main",
                "open",
                1,
                18,
                3,
                1,
                "https://github.com/openai/pr-review-agent/pull/" + prNumber,
                List.of(new GitHubChangedFileDTO(
                        "backend/src/main/java/com/example/prreview/controller/ReviewController.java",
                        "added",
                        18,
                        0,
                        18,
                        "@@ -0,0 +1,3 @@\n+@RestController\n+@RequestMapping(\"/api/reviews\")\n+public class ReviewController {}",
                        null
                ))
        );

        ChatCompletionResponse response = new ChatCompletionResponse();
        response.setModel("test-model");
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        choice.setMessage(new ChatMessage("assistant", aiResponseJson));
        choice.setFinishReason("stop");
        response.setChoices(List.of(choice));

        given(gitHubClient.parseRepository("https://github.com/openai/pr-review-agent")).willReturn(repository);
        given(gitHubClient.getPullRequest("https://github.com/openai/pr-review-agent", repository, prNumber))
                .willReturn(pullRequest);
        given(modelClient.chatCompletion(any(ReviewTask.class), eq("FINAL_REPORT"), any()))
                .willReturn(response);
    }

    private ReviewTask saveTask(
            String repoUrl,
            String repoOwner,
            String repoName,
            Integer prNumber,
            TaskStatus status,
            Integer riskCount,
            String errorMessage
    ) {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl(repoUrl);
        task.setRepoOwner(repoOwner);
        task.setRepoName(repoName);
        task.setPrNumber(prNumber);
        task.setStatus(status);
        task.setRiskCount(riskCount);
        task.setErrorMessage(errorMessage);
        task.setCreatedAt(LocalDateTime.of(2026, 5, 29, 10, prNumber));
        return reviewTaskRepository.save(task);
    }

    private void saveReviewResult(ReviewTask task, String summary, String overallConclusion) {
        ReviewResult reviewResult = new ReviewResult();
        reviewResult.setTask(task);
        reviewResult.setSummary(summary);
        reviewResult.setChangedModules("[]");
        reviewResult.setReviewSuggestions("[]");
        reviewResult.setTestSuggestions("[]");
        reviewResult.setOverallConclusion(overallConclusion);
        reviewResultRepository.save(reviewResult);
    }
}
