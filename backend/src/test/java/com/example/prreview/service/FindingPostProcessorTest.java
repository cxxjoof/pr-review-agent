package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.dto.diff.ChangedFileContext;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.review.AiReviewReportDTO;
import com.example.prreview.dto.review.RiskItemDTO;
import com.example.prreview.enums.FileChangeType;
import com.example.prreview.enums.PrType;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingPostProcessorTest {

    private final FindingPostProcessor findingPostProcessor = new FindingPostProcessor();

    @Test
    void shouldDeduplicateFindingsByFileLineAndCategoryEvenWhenTitlesDiffer() {
        RiskItemDTO firstFinding = new RiskItemDTO();
        firstFinding.setFilePath("backend/src/main/java/com/example/prreview/service/ReviewTaskService.java");
        firstFinding.setLineNumber(92);
        firstFinding.setFindingLevel("MEDIUM");
        firstFinding.setFindingKind("RISK");
        firstFinding.setFindingCategory("EXCEPTION_HANDLING");
        firstFinding.setTitle("Handle null feedback status");
        firstFinding.setDescription("The same issue described with one title.");

        RiskItemDTO duplicateFinding = new RiskItemDTO();
        duplicateFinding.setFilePath("backend/src/main/java/com/example/prreview/service/ReviewTaskService.java");
        duplicateFinding.setLineNumber(92);
        duplicateFinding.setFindingLevel("MEDIUM");
        duplicateFinding.setFindingKind("RISK");
        duplicateFinding.setFindingCategory("EXCEPTION_HANDLING");
        duplicateFinding.setTitle("Guard latest feedback lookup");
        duplicateFinding.setDescription("The same issue described with another title.");

        AiReviewReportDTO report = new AiReviewReportDTO();
        report.setFindings(List.of(firstFinding, duplicateFinding));

        AiReviewReportDTO processed = findingPostProcessor.postProcess(buildReviewContext(), report);

        assertThat(processed.getFindings()).hasSize(1);
        assertThat(processed.getFindings().get(0).getTitle()).isEqualTo("Guard latest feedback lookup");
    }

    private ReviewContext buildReviewContext() {
        ChangedFileContext fileContext = new ChangedFileContext(
                "backend/src/main/java/com/example/prreview/service/ReviewTaskService.java",
                null,
                FileChangeType.MODIFIED,
                "BACKEND",
                1,
                1,
                2,
                1,
                1,
                1,
                0,
                true,
                null,
                List.of(),
                List.of(),
                List.of()
        );

        return new ReviewContext(
                1L,
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                12,
                "https://github.com/openai/pr-review-agent/pull/12",
                PrType.CODE,
                "fix: reduce duplicate findings",
                null,
                "octocat",
                "feature/high-signal",
                "main",
                1,
                1,
                1,
                1,
                List.of("backend/service"),
                1,
                0,
                1,
                1,
                0,
                "SMALL",
                List.of(fileContext),
                "PR Overview"
        );
    }
}
