package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.prreview.client.ModelClient;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.dto.model.ChatMessage;
import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.PrType;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.ReviewTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AiReviewServiceTest {

    @Mock
    private ReviewTaskRepository reviewTaskRepository;

    @Mock
    private PromptBuildService promptBuildService;

    @Mock
    private ModelClient modelClient;

    @Mock
    private ReviewReportService reviewReportService;

    @Mock
    private RiskItemService riskItemService;

    @Mock
    private FindingPostProcessor findingPostProcessor;

    @InjectMocks
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        aiReviewService = new AiReviewService(
                reviewTaskRepository,
                promptBuildService,
                modelClient,
                reviewReportService,
                riskItemService,
                findingPostProcessor,
                new ObjectMapper()
        );
    }

    @Test
    void shouldGenerateAndPersistAiReviewReport() {
        ReviewTask task = buildTask();
        ReviewContext reviewContext = buildContext();
        ChatCompletionRequest request = buildRequest();

        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(reviewTaskRepository.save(any(ReviewTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptBuildService.buildReviewRequest(reviewContext)).thenReturn(request);
        when(modelClient.chatCompletion(task, AiReviewService.AI_REVIEW_CALL_TYPE, request))
                .thenReturn(buildResponse("""
                        {
                          "summary": "Adds AI review orchestration.",
                          "changedModules": ["backend/service", "backend/dto"],
                          "findings": [
                            {
                              "filePath": "backend/src/main/java/com/example/prreview/service/AiReviewService.java",
                              "lineNumber": 48,
                              "findingLevel": "MEDIUM",
                              "findingKind": "RISK",
                              "findingCategory": "TEST_GAP",
                              "title": "补充测试覆盖",
                              "description": "新的编排路径建议补充测试覆盖。",
                              "suggestion": "为成功和失败流程增加服务层测试。",
                              "confidence": 0.88
                            }
                          ],
                          "reviewSuggestions": ["Verify the fallback path for invalid model JSON."],
                          "testSuggestions": ["Add a unit test for model failure handling."],
                          "overallConclusion": "The implementation is sound with minor follow-up testing work."
                        }
                        """));
        when(findingPostProcessor.postProcess(eq(reviewContext), any(AiReviewReportDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        AiReviewReportDTO report = aiReviewService.analyze(reviewContext);

        assertThat(report.getSummary()).isEqualTo("Adds AI review orchestration.");
        assertThat(report.getFindings()).hasSize(1);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(task.getRiskCount()).isEqualTo(1);
        assertThat(task.getPrType()).isEqualTo(PrType.CODE);
        verify(reviewReportService).saveOrUpdate(eq(task), any(AiReviewReportDTO.class), any(String.class));
        verify(riskItemService).replaceRiskItems(eq(task), any(List.class));
    }

    @Test
    void shouldFallbackWhenModelReturnsInvalidJson() {
        ReviewTask task = buildTask();
        ReviewContext reviewContext = buildContext();
        ChatCompletionRequest request = buildRequest();

        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(reviewTaskRepository.save(any(ReviewTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptBuildService.buildReviewRequest(reviewContext)).thenReturn(request);
        when(modelClient.chatCompletion(task, AiReviewService.AI_REVIEW_CALL_TYPE, request))
                .thenReturn(buildResponse("not valid json"));
        when(findingPostProcessor.postProcess(eq(reviewContext), any(AiReviewReportDTO.class)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        AiReviewReportDTO report = aiReviewService.analyze(reviewContext);

        assertThat(report.getFindings()).isEmpty();
        assertThat(report.getChangedModules()).containsExactly("backend/service");
        assertThat(report.getOverallConclusion()).contains("AI");
        assertThat(report.getReviewSuggestions()).isNotEmpty();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(task.getRiskCount()).isZero();
        verify(reviewReportService).saveOrUpdate(eq(task), any(AiReviewReportDTO.class), eq("not valid json"));
        verify(riskItemService).replaceRiskItems(task, List.of());
    }

    @Test
    void shouldMarkTaskAsFailedWhenModelInvocationFails() {
        ReviewTask task = buildTask();
        ReviewContext reviewContext = buildContext();
        ChatCompletionRequest request = buildRequest();

        when(reviewTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(reviewTaskRepository.save(any(ReviewTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promptBuildService.buildReviewRequest(reviewContext)).thenReturn(request);
        when(modelClient.chatCompletion(task, AiReviewService.AI_REVIEW_CALL_TYPE, request))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Model API request failed."));

        assertThatThrownBy(() -> aiReviewService.analyze(reviewContext))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("Model API request failed.");
        verify(reviewReportService, never()).saveOrUpdate(any(), any(), any());
        verify(riskItemService, never()).replaceRiskItems(any(), any());
    }

    private ReviewTask buildTask() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setRepoUrl("https://github.com/openai/pr-review-agent");
        task.setRepoOwner("openai");
        task.setRepoName("pr-review-agent");
        task.setPrNumber(8);
        task.setStatus(TaskStatus.SUCCESS);
        task.setRiskCount(0);
        return task;
    }

    private ReviewContext buildContext() {
        return new ReviewContext(
                1L,
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                8,
                "https://github.com/openai/pr-review-agent/pull/8",
                PrType.CODE,
                "feat: add ai review analysis",
                "Generate AI review reports",
                "octocat",
                "feature/ai-review",
                "main",
                2,
                20,
                5,
                1,
                List.of("backend/service"),
                2,
                0,
                16,
                4,
                6,
                "SMALL",
                List.of(),
                "PR Overview\nChanged Files\n- File: backend/src/main/java/com/example/prreview/service/AiReviewService.java"
        );
    }

    private ChatCompletionRequest buildRequest() {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessages(List.of(new ChatMessage("user", "Analyze this pull request.")));
        return request;
    }

    private ChatCompletionResponse buildResponse(String content) {
        ChatCompletionResponse response = new ChatCompletionResponse();
        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setMessage(new ChatMessage("assistant", content));
        response.setChoices(List.of(choice));
        return response;
    }
}
