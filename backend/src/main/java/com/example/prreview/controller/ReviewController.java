package com.example.prreview.controller;

import com.example.prreview.dto.request.CreateReviewRequest;
import com.example.prreview.dto.response.ApiResponse;
import com.example.prreview.dto.response.ReviewResultResponse;
import com.example.prreview.dto.response.ReviewTaskResponse;
import com.example.prreview.service.ReviewTaskService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewTaskService reviewTaskService;

    public ReviewController(ReviewTaskService reviewTaskService) {
        this.reviewTaskService = reviewTaskService;
    }

    @PostMapping
    public ApiResponse<ReviewTaskResponse> createReviewTask(@RequestBody CreateReviewRequest request) {
        return ApiResponse.success(reviewTaskService.createTask(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewResultResponse> getReviewTask(@PathVariable Long id) {
        return ApiResponse.success(reviewTaskService.getTaskById(id));
    }

    @GetMapping
    public ApiResponse<List<ReviewTaskResponse>> listReviewTasks() {
        return ApiResponse.success(reviewTaskService.listTasks());
    }
}
