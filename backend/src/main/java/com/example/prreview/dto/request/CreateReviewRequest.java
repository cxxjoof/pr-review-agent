package com.example.prreview.dto.request;

import com.example.prreview.validator.RepoUrlValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateReviewRequest(
        @NotBlank(message = "repoUrl must not be blank.")
        @Pattern(
                regexp = RepoUrlValidator.GITHUB_REPOSITORY_URL_REGEX,
                message = "repoUrl must be a valid GitHub repository URL."
        )
        String repoUrl,
        @NotNull(message = "prNumber must not be null.")
        @Positive(message = "prNumber must be a positive number.")
        Integer prNumber
) {
}
