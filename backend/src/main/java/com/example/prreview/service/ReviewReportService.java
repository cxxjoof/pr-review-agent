package com.example.prreview.service;

import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.entity.ReviewResult;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.repository.ReviewResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewReportService {

    private final ReviewResultRepository reviewResultRepository;
    private final ObjectMapper objectMapper;

    public ReviewReportService(
            ReviewResultRepository reviewResultRepository,
            ObjectMapper objectMapper
    ) {
        this.reviewResultRepository = reviewResultRepository;
        this.objectMapper = objectMapper;
    }

    public ReviewResult saveOrUpdate(ReviewTask task, AiReviewReportDTO report, String rawAiResponse) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("Persisted review task is required.");
        }
        if (report == null) {
            throw new IllegalArgumentException("AI review report must not be null.");
        }

        ReviewResult reviewResult = reviewResultRepository.findByTaskId(task.getId()).orElseGet(ReviewResult::new);
        reviewResult.setTask(task);
        reviewResult.setSummary(report.getSummary());
        reviewResult.setChangedModules(writeJson(report.getChangedModules()));
        reviewResult.setReviewSuggestions(writeJson(report.getReviewSuggestions()));
        reviewResult.setTestSuggestions(writeJson(report.getTestSuggestions()));
        reviewResult.setOverallConclusion(report.getOverallConclusion());
        reviewResult.setRawAiResponse(rawAiResponse);
        return reviewResultRepository.save(reviewResult);
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI review report JSON fields.", exception);
        }
    }
}
