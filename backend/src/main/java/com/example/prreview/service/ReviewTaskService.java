package com.example.prreview.service;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.dto.request.CreateReviewRequest;
import com.example.prreview.dto.response.ReviewResultResponse;
import com.example.prreview.dto.response.ReviewTaskResponse;
import com.example.prreview.entity.PullRequestInfo;
import com.example.prreview.entity.ReviewResult;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.repository.PullRequestInfoRepository;
import com.example.prreview.repository.ReviewResultRepository;
import com.example.prreview.repository.ReviewTaskRepository;
import com.example.prreview.repository.RiskItemRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewTaskService {

    private final GitHubPrService gitHubPrService;
    private final ReviewContextBuildService reviewContextBuildService;
    private final AiReviewService aiReviewService;
    private final ReviewTaskRepository reviewTaskRepository;
    private final PullRequestInfoRepository pullRequestInfoRepository;
    private final ReviewResultRepository reviewResultRepository;
    private final RiskItemRepository riskItemRepository;
    private final ObjectMapper objectMapper;

    public ReviewTaskService(
            GitHubPrService gitHubPrService,
            ReviewContextBuildService reviewContextBuildService,
            AiReviewService aiReviewService,
            ReviewTaskRepository reviewTaskRepository,
            PullRequestInfoRepository pullRequestInfoRepository,
            ReviewResultRepository reviewResultRepository,
            RiskItemRepository riskItemRepository,
            ObjectMapper objectMapper
    ) {
        this.gitHubPrService = gitHubPrService;
        this.reviewContextBuildService = reviewContextBuildService;
        this.aiReviewService = aiReviewService;
        this.reviewTaskRepository = reviewTaskRepository;
        this.pullRequestInfoRepository = pullRequestInfoRepository;
        this.reviewResultRepository = reviewResultRepository;
        this.riskItemRepository = riskItemRepository;
        this.objectMapper = objectMapper;
    }

    public ReviewTaskResponse createTask(CreateReviewRequest request) {
        validateCreateRequest(request);

        GitHubPullRequestDTO pullRequest = gitHubPrService.fetchAndStorePullRequest(
                request.repoUrl().trim(),
                request.prNumber()
        );
        ReviewContext reviewContext = reviewContextBuildService.buildReviewContext(pullRequest);
        aiReviewService.analyze(reviewContext);

        ReviewTask task = findTask(reviewContext.taskId());
        ReviewResult reviewResult = reviewResultRepository.findByTaskId(task.getId()).orElse(null);
        return toTaskResponse(task, reviewResult);
    }

    public ReviewResultResponse getTaskById(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId must be a positive number.");
        }

        ReviewTask task = findTask(taskId);
        PullRequestInfo pullRequestInfo = pullRequestInfoRepository.findByTaskId(taskId).orElse(null);
        ReviewResult reviewResult = reviewResultRepository.findByTaskId(taskId).orElse(null);
        List<RiskItem> riskItems = riskItemRepository.findByTaskId(taskId);

        return new ReviewResultResponse(
                task.getId(),
                task.getStatus().name(),
                task.getRiskCount(),
                task.getErrorMessage(),
                toPullRequestPayload(task, pullRequestInfo),
                toReviewReportPayload(reviewResult),
                riskItems.stream().map(this::toRiskItemPayload).toList(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public List<ReviewTaskResponse> listTasks() {
        return reviewTaskRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(task -> toTaskResponse(task, reviewResultRepository.findByTaskId(task.getId()).orElse(null)))
                .toList();
    }

    private void validateCreateRequest(CreateReviewRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body must not be null.");
        }
        if (!StringUtils.hasText(request.repoUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "repoUrl must not be blank.");
        }
        if (request.prNumber() == null || request.prNumber() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prNumber must be a positive number.");
        }
    }

    private ReviewTask findTask(Long taskId) {
        return reviewTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review task not found."));
    }

    private ReviewTaskResponse toTaskResponse(ReviewTask task, ReviewResult reviewResult) {
        return new ReviewTaskResponse(
                task.getId(),
                task.getRepoUrl(),
                task.getRepoOwner(),
                task.getRepoName(),
                task.getPrNumber(),
                task.getStatus().name(),
                task.getRiskCount(),
                reviewResult == null ? null : reviewResult.getSummary(),
                reviewResult == null ? null : reviewResult.getOverallConclusion(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private ReviewResultResponse.PullRequestPayload toPullRequestPayload(ReviewTask task, PullRequestInfo pullRequestInfo) {
        return new ReviewResultResponse.PullRequestPayload(
                task.getRepoUrl(),
                task.getRepoOwner(),
                task.getRepoName(),
                task.getPrNumber(),
                pullRequestInfo == null ? null : pullRequestInfo.getTitle(),
                pullRequestInfo == null ? null : pullRequestInfo.getDescription(),
                pullRequestInfo == null ? null : pullRequestInfo.getAuthor(),
                pullRequestInfo == null ? null : pullRequestInfo.getSourceBranch(),
                pullRequestInfo == null ? null : pullRequestInfo.getTargetBranch(),
                pullRequestInfo == null ? null : pullRequestInfo.getState(),
                pullRequestInfo == null ? null : pullRequestInfo.getChangedFiles(),
                pullRequestInfo == null ? null : pullRequestInfo.getAdditions(),
                pullRequestInfo == null ? null : pullRequestInfo.getDeletions(),
                pullRequestInfo == null ? null : pullRequestInfo.getCommits(),
                pullRequestInfo == null ? null : pullRequestInfo.getPrUrl()
        );
    }

    private ReviewResultResponse.ReviewReportPayload toReviewReportPayload(ReviewResult reviewResult) {
        if (reviewResult == null) {
            return new ReviewResultResponse.ReviewReportPayload(null, List.of(), List.of(), List.of(), null);
        }

        return new ReviewResultResponse.ReviewReportPayload(
                reviewResult.getSummary(),
                readJsonList(reviewResult.getChangedModules()),
                readJsonList(reviewResult.getReviewSuggestions()),
                readJsonList(reviewResult.getTestSuggestions()),
                reviewResult.getOverallConclusion()
        );
    }

    private ReviewResultResponse.RiskItemPayload toRiskItemPayload(RiskItem riskItem) {
        return new ReviewResultResponse.RiskItemPayload(
                riskItem.getFilePath(),
                riskItem.getLineNumber(),
                riskItem.getCodeSnippet(),
                riskItem.getRiskLevel() == null ? null : riskItem.getRiskLevel().name(),
                riskItem.getRiskType() == null ? null : riskItem.getRiskType().name(),
                riskItem.getDescription(),
                riskItem.getSuggestion(),
                riskItem.getConfidence()
        );
    }

    private List<String> readJsonList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception exception) {
            String nestedJson = tryReadNestedJsonString(json);
            if (nestedJson != null) {
                try {
                    return objectMapper.readValue(nestedJson, new TypeReference<List<String>>() {
                    });
                } catch (Exception ignored) {
                    // Fallback to lightweight parsing below.
                }
            }

            return fallbackSplitJsonArray(json);
        }
    }

    private String tryReadNestedJsonString(String json) {
        try {
            return objectMapper.readValue(json, String.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private List<String> fallbackSplitJsonArray(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isBlank()) {
            return List.of();
        }

        return List.of(body.split(",")).stream()
                .map(String::trim)
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .filter(StringUtils::hasText)
                .toList();
    }
}
