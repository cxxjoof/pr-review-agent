package com.example.prreview.service;

import com.example.prreview.client.ModelClient;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.ReviewTaskRepository;
import com.example.prreview.util.JsonParseUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiReviewService {

    static final String AI_REVIEW_CALL_TYPE = "FINAL_REPORT";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final ReviewTaskRepository reviewTaskRepository;
    private final PromptBuildService promptBuildService;
    private final ModelClient modelClient;
    private final ReviewReportService reviewReportService;
    private final RiskItemService riskItemService;
    private final ObjectMapper objectMapper;

    public AiReviewService(
            ReviewTaskRepository reviewTaskRepository,
            PromptBuildService promptBuildService,
            ModelClient modelClient,
            ReviewReportService reviewReportService,
            RiskItemService riskItemService,
            ObjectMapper objectMapper
    ) {
        this.reviewTaskRepository = reviewTaskRepository;
        this.promptBuildService = promptBuildService;
        this.modelClient = modelClient;
        this.reviewReportService = reviewReportService;
        this.riskItemService = riskItemService;
        this.objectMapper = objectMapper;
    }

    public AiReviewReportDTO analyze(ReviewContext reviewContext) {
        validateContext(reviewContext);
        ReviewTask task = reviewTaskRepository.findById(reviewContext.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Review task not found: " + reviewContext.taskId()));

        task.setStatus(TaskStatus.RUNNING);
        task.setErrorMessage(null);
        reviewTaskRepository.save(task);

        try {
            ChatCompletionResponse response = modelClient.chatCompletion(
                    task,
                    AI_REVIEW_CALL_TYPE,
                    promptBuildService.buildReviewRequest(reviewContext)
            );
            String rawAiResponse = response.getFirstMessageContent();
            AiReviewReportDTO report = parseReport(rawAiResponse, reviewContext.changedModules());

            reviewReportService.saveOrUpdate(task, report, rawAiResponse);
            riskItemService.replaceRiskItems(task, report.getRiskItems());

            task.setRiskCount(report.getRiskItems().size());
            task.setStatus(TaskStatus.SUCCESS);
            task.setErrorMessage(null);
            reviewTaskRepository.save(task);
            return report;
        } catch (RuntimeException exception) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(extractErrorMessage(exception));
            reviewTaskRepository.save(task);
            throw exception;
        }
    }

    private void validateContext(ReviewContext reviewContext) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null.");
        }
        if (reviewContext.taskId() == null) {
            throw new IllegalArgumentException("Review context must include a task id.");
        }
    }

    private AiReviewReportDTO parseReport(String rawAiResponse, List<String> defaultChangedModules) {
        try {
            return JsonParseUtils.readJsonObject(objectMapper, rawAiResponse, AiReviewReportDTO.class)
                    .normalize(defaultChangedModules);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return buildFallbackReport(defaultChangedModules);
        }
    }

    private AiReviewReportDTO buildFallbackReport(List<String> defaultChangedModules) {
        AiReviewReportDTO fallback = new AiReviewReportDTO();
        fallback.setSummary("The model returned an unparsable review report, so a fallback summary was generated.");
        fallback.setChangedModules(defaultChangedModules == null ? List.of() : defaultChangedModules);
        fallback.setReviewSuggestions(List.of(
                "Retry the AI review after verifying the model returns the required JSON structure."
        ));
        fallback.setTestSuggestions(List.of(
                "Re-run the review flow and confirm the model output can be parsed into structured review data."
        ));
        fallback.setOverallConclusion("AI analysis completed, but the structured review payload could not be parsed reliably.");
        return fallback.normalize(defaultChangedModules);
    }

    private String extractErrorMessage(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException
                && StringUtils.hasText(responseStatusException.getReason())) {
            return compactErrorMessage(responseStatusException.getReason());
        }

        return compactErrorMessage(exception == null ? null : exception.getMessage());
    }

    private String compactErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return "AI review analysis failed.";
        }

        String compacted = errorMessage.replaceAll("\\s+", " ").trim();
        return compacted.length() > MAX_ERROR_MESSAGE_LENGTH
                ? compacted.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "..."
                : compacted;
    }
}
