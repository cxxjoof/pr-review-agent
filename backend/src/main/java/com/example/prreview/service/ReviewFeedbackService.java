package com.example.prreview.service;

import com.example.prreview.dto.request.CreateReviewFeedbackRequest;
import com.example.prreview.dto.response.ReviewFeedbackResponse;
import com.example.prreview.entity.ReviewFeedback;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.entity.RiskItem;
import com.example.prreview.exception.BusinessException;
import com.example.prreview.repository.ReviewFeedbackRepository;
import com.example.prreview.repository.ReviewTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReviewFeedbackService {

    private final ReviewTaskRepository reviewTaskRepository;
    private final RiskItemService riskItemService;
    private final ReviewFeedbackRepository reviewFeedbackRepository;

    public ReviewFeedbackService(
            ReviewTaskRepository reviewTaskRepository,
            RiskItemService riskItemService,
            ReviewFeedbackRepository reviewFeedbackRepository
    ) {
        this.reviewTaskRepository = reviewTaskRepository;
        this.riskItemService = riskItemService;
        this.reviewFeedbackRepository = reviewFeedbackRepository;
    }

    public ReviewFeedbackResponse createFeedback(Long taskId, Long findingId, CreateReviewFeedbackRequest request) {
        if (taskId == null || taskId <= 0) {
            throw BusinessException.validation("taskId must be a positive number.");
        }
        if (findingId == null || findingId <= 0) {
            throw BusinessException.validation("findingId must be a positive number.");
        }
        if (request == null || request.feedbackType() == null) {
            throw BusinessException.validation("feedbackType must not be null.");
        }

        ReviewTask task = reviewTaskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("Review task not found."));
        RiskItem finding = riskItemService.findByTaskIdAndFindingId(taskId, findingId);

        ReviewFeedback feedback = new ReviewFeedback();
        feedback.setTask(task);
        feedback.setFinding(finding);
        feedback.setFeedbackType(request.feedbackType());
        feedback.setComment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null);
        ReviewFeedback savedFeedback = reviewFeedbackRepository.save(feedback);

        finding.setFeedbackStatus(savedFeedback.getFeedbackType());
        riskItemService.save(finding);

        return new ReviewFeedbackResponse(
                taskId,
                findingId,
                savedFeedback.getFeedbackType().name(),
                savedFeedback.getComment(),
                savedFeedback.getCreatedAt()
        );
    }
}
