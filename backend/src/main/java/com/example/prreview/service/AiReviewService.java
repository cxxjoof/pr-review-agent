package com.example.prreview.service;

import com.example.prreview.client.ModelClient;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.exception.BusinessException;
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
    private final FindingPostProcessor findingPostProcessor;
    private final ObjectMapper objectMapper;

    public AiReviewService(
            ReviewTaskRepository reviewTaskRepository,
            PromptBuildService promptBuildService,
            ModelClient modelClient,
            ReviewReportService reviewReportService,
            RiskItemService riskItemService,
            FindingPostProcessor findingPostProcessor,
            ObjectMapper objectMapper
    ) {
        this.reviewTaskRepository = reviewTaskRepository;
        this.promptBuildService = promptBuildService;
        this.modelClient = modelClient;
        this.reviewReportService = reviewReportService;
        this.riskItemService = riskItemService;
        this.findingPostProcessor = findingPostProcessor;
        this.objectMapper = objectMapper;
    }

    public AiReviewReportDTO analyze(ReviewContext reviewContext) {
        validateContext(reviewContext);
        ReviewTask task = reviewTaskRepository.findById(reviewContext.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Review task not found: " + reviewContext.taskId()));

        task.setStatus(TaskStatus.RUNNING);
        task.setPrType(reviewContext.prType());
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
            report = findingPostProcessor.postProcess(reviewContext, report);

            reviewReportService.saveOrUpdate(task, report, rawAiResponse);
            riskItemService.replaceRiskItems(task, report.getFindings());

            task.setRiskCount(report.getFindings().size());
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
        fallback.setSummary("模型返回内容无法按预期解析，系统已生成一份兜底评审摘要。");
        fallback.setChangedModules(defaultChangedModules == null ? List.of() : defaultChangedModules);
        fallback.setReviewSuggestions(List.of(
                "请检查模型是否严格按照约定的 JSON 结构返回结果，然后重新发起 AI 评审。"
        ));
        fallback.setTestSuggestions(List.of(
                "建议重新执行评审流程，并确认模型输出能够被解析为结构化评审数据。"
        ));
        fallback.setOverallConclusion("AI 分析已执行完成，但结构化评审结果解析失败，当前展示的是兜底结果。");
        return fallback.normalize(defaultChangedModules);
    }

    private String extractErrorMessage(RuntimeException exception) {
        if (exception instanceof BusinessException businessException
                && StringUtils.hasText(businessException.getMessage())) {
            return compactErrorMessage(businessException.getMessage());
        }
        if (exception instanceof ResponseStatusException responseStatusException
                && StringUtils.hasText(responseStatusException.getReason())) {
            return compactErrorMessage(responseStatusException.getReason());
        }

        return compactErrorMessage(exception == null ? null : exception.getMessage());
    }

    private String compactErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return "AI Review 分析失败。";
        }

        String compacted = errorMessage.replaceAll("\\s+", " ").trim();
        return compacted.length() > MAX_ERROR_MESSAGE_LENGTH
                ? compacted.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "..."
                : compacted;
    }
}
