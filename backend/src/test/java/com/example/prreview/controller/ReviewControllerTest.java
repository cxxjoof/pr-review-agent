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
import com.example.prreview.enums.PrType;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.ModelCallLogRepository;
import com.example.prreview.repository.PullRequestInfoRepository;
import com.example.prreview.repository.ReviewFeedbackRepository;
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

    @Autowired
    private ReviewFeedbackRepository reviewFeedbackRepository;

    @MockBean
    private GitHubClient gitHubClient;

    @MockBean
    private ModelClient modelClient;

    @BeforeEach
    void setUp() {
        reviewFeedbackRepository.deleteAll();
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
                "docs: improve readme command formatting",
                """
                {
                  "summary": "本次 PR 主要优化 README 中的命令展示格式。",
                  "changedModules": ["README.md"],
                  "findings": [
                    {
                      "filePath": "README.md",
                      "lineNumber": 42,
                      "findingLevel": "MEDIUM",
                      "findingKind": "RISK",
                      "findingCategory": "DOCUMENTATION_FORMAT",
                      "title": "命令与说明缺少空行",
                      "description": "命令与后续说明文本贴在一起，可能影响阅读体验。",
                      "suggestion": "建议使用 Markdown 代码块或空行分隔命令与说明。",
                      "beforeExample": "mkdir demoCreates a directory",
                      "afterExample": "mkdir demo\\n\\nCreates a directory",
                      "confidence": 0.88
                    }
                  ],
                  "reviewSuggestions": ["检查 README 在 GitHub 上的渲染效果。"],
                  "testSuggestions": ["本地复制 README 命令验证可执行性。"],
                  "overallConclusion": "本次 PR 为文档改进，建议优先修正文档可读性问题。"
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
                .andExpect(jsonPath("$.data.prType").value("DOCUMENTATION"))
                .andExpect(jsonPath("$.data.resultViewType").value("DOCUMENTATION_FINDINGS"))
                .andExpect(jsonPath("$.data.findingCount").value(1))
                .andExpect(jsonPath("$.data.summary").value("本次 PR 主要优化 README 中的命令展示格式。"));

        List<ReviewTask> tasks = reviewTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(tasks.get(0).getRiskCount()).isEqualTo(1);
        assertThat(tasks.get(0).getPrType()).isEqualTo(PrType.DOCUMENTATION);

        PullRequestInfo pullRequestInfo = pullRequestInfoRepository.findByTaskId(tasks.get(0).getId()).orElseThrow();
        assertThat(pullRequestInfo.getTitle()).isEqualTo("docs: improve readme command formatting");

        ReviewResult reviewResult = reviewResultRepository.findByTaskId(tasks.get(0).getId()).orElseThrow();
        assertThat(reviewResult.getSummary()).isEqualTo("本次 PR 主要优化 README 中的命令展示格式。");

        List<RiskItem> findings = riskItemRepository.findByTaskId(tasks.get(0).getId());
        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getFindingLevel().name()).isEqualTo("LOW");
    }

    @Test
    void shouldReturnReviewTaskDetailAndFindingPayload() throws Exception {
        mockReviewFlow(
                12,
                "feat: expose review detail api",
                """
                {
                  "summary": "This PR exposes review detail APIs.",
                  "changedModules": ["backend/controller"],
                  "findings": [
                    {
                      "filePath": "backend/src/main/java/com/example/prreview/controller/ReviewController.java",
                      "lineNumber": 21,
                      "findingLevel": "MEDIUM",
                      "findingKind": "RISK",
                      "findingCategory": "EXCEPTION_HANDLING",
                      "title": "建议补充异常路径验证",
                      "description": "接口新增后建议确认异常路径是否返回统一错误结构。",
                      "suggestion": "为外部依赖失败场景补充控制器集成测试。",
                      "confidence": 0.92
                    }
                  ],
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
                .andExpect(jsonPath("$.data.prType").value("CODE"))
                .andExpect(jsonPath("$.data.resultViewType").value("CODE_RISKS"))
                .andExpect(jsonPath("$.data.pullRequest.repoOwner").value("openai"))
                .andExpect(jsonPath("$.data.pullRequest.repoName").value("pr-review-agent"))
                .andExpect(jsonPath("$.data.pullRequest.title").value("feat: expose review detail api"))
                .andExpect(jsonPath("$.data.reviewResult.summary").value("This PR exposes review detail APIs."))
                .andExpect(jsonPath("$.data.reviewResult.changedModules[0]").value("backend/controller"))
                .andExpect(jsonPath("$.data.reviewResult.reviewSuggestions[0]")
                        .value("Confirm response fields stay backward compatible."))
                .andExpect(jsonPath("$.data.findings[0].findingLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.data.findings[0].findingCategory").value("EXCEPTION_HANDLING"))
                .andExpect(jsonPath("$.data.findings[0].feedbackStatus").doesNotExist());
    }

    @Test
    void shouldCreateFeedbackForFinding() throws Exception {
        mockReviewFlow(
                13,
                "feat: add feedback endpoint",
                """
                {
                  "summary": "This PR adds feedback flow.",
                  "changedModules": ["backend/controller"],
                  "findings": [
                    {
                      "filePath": "backend/src/main/java/com/example/prreview/controller/ReviewController.java",
                      "lineNumber": 34,
                      "findingLevel": "LOW",
                      "findingKind": "ADVISORY",
                      "findingCategory": "TEST_GAP",
                      "title": "建议补充反馈接口测试",
                      "description": "建议确认反馈接口在错误输入下的响应行为。",
                      "suggestion": "增加反馈接口参数校验测试。",
                      "confidence": 0.72
                    }
                  ],
                  "reviewSuggestions": ["Review the new feedback endpoint carefully."],
                  "testSuggestions": ["Add feedback endpoint integration tests."],
                  "overallConclusion": "Follow-up validation is recommended."
                }
                """
        );

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repoUrl": "https://github.com/openai/pr-review-agent",
                                  "prNumber": 13
                                }
                                """))
                .andExpect(status().isOk());

        ReviewTask task = reviewTaskRepository.findAll().getFirst();
        RiskItem finding = riskItemRepository.findByTaskId(task.getId()).getFirst();

        mockMvc.perform(post("/api/reviews/{taskId}/findings/{findingId}/feedback", task.getId(), finding.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feedbackType": "USEFUL",
                                  "comment": "This helped."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(task.getId()))
                .andExpect(jsonPath("$.data.findingId").value(finding.getId()))
                .andExpect(jsonPath("$.data.feedbackType").value("USEFUL"))
                .andExpect(jsonPath("$.data.comment").value("This helped."));

        mockMvc.perform(get("/api/reviews/{id}", task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.findings[0].feedbackStatus").value("USEFUL"));
    }

    @Test
    void shouldListReviewTasks() throws Exception {
        ReviewTask olderTask = saveTask(
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                1,
                PrType.CODE,
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
                PrType.DOCUMENTATION,
                TaskStatus.FAILED,
                0,
                "Model call failed"
        );

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].taskId").value(newerTask.getId()))
                .andExpect(jsonPath("$.data[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data[0].prType").value("DOCUMENTATION"))
                .andExpect(jsonPath("$.data[0].errorMessage").value("Model call failed"))
                .andExpect(jsonPath("$.data[1].taskId").value(olderTask.getId()))
                .andExpect(jsonPath("$.data[1].summary").value("Older review summary"))
                .andExpect(jsonPath("$.data[1].overallConclusion").value("Earlier conclusion"));
    }

    @Test
    void shouldReturnValidationErrorWhenCreateRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "repoUrl": "https://gitlab.com/openai/pr-review-agent",
                                  "prNumber": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("repoUrl must be a valid GitHub repository URL.")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("prNumber must be a positive number.")));
    }

    @Test
    void shouldReturnValidationErrorWhenTaskIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/reviews/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("taskId must be a positive number."));
    }

    private void mockReviewFlow(int prNumber, String title, String aiResponseJson) {
        GitHubClient.GitHubRepository repository = new GitHubClient.GitHubRepository("openai", "pr-review-agent");
        String fileName = title.startsWith("docs:") ? "README.md" : "backend/src/main/java/com/example/prreview/controller/ReviewController.java";
        String patch = title.startsWith("docs:")
                ? "@@ -40,1 +40,3 @@\n+mkdir demoCreates a directory\n+\n+More explanation"
                : "@@ -0,0 +1,3 @@\n+@RestController\n+@RequestMapping(\"/api/reviews\")\n+public class ReviewController {}";
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
                        fileName,
                        "modified",
                        18,
                        3,
                        21,
                        patch,
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
            PrType prType,
            TaskStatus status,
            Integer findingCount,
            String errorMessage
    ) {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl(repoUrl);
        task.setRepoOwner(repoOwner);
        task.setRepoName(repoName);
        task.setPrNumber(prNumber);
        task.setPrType(prType);
        task.setStatus(status);
        task.setRiskCount(findingCount);
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
