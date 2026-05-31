package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.enums.PrType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewContextBuildServiceTest {

    private final ReviewContextBuildService reviewContextBuildService =
            new ReviewContextBuildService(new DiffParseService());

    @Test
    void shouldBuildStructuredReviewContextForAiAnalysis() {
        GitHubPullRequestDTO pullRequest = new GitHubPullRequestDTO(
                1L,
                101L,
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                8,
                "feat: add review context builder",
                "Prepare structured diff context for AI review",
                "octocat",
                "feature/context-builder",
                "main",
                "open",
                2,
                4,
                1,
                1,
                "https://github.com/openai/pr-review-agent/pull/8",
                List.of(
                        new GitHubChangedFileDTO(
                                "backend/src/main/java/com/example/prreview/service/ReviewContextBuildService.java",
                                "added",
                                4,
                                0,
                                4,
                                """
                                @@ -0,0 +1,4 @@
                                +package com.example.prreview.service;
                                +
                                +public class ReviewContextBuildService {
                                +}
                                """,
                                null
                        ),
                        new GitHubChangedFileDTO(
                                "docs/架构设计.md",
                                "modified",
                                0,
                                0,
                                0,
                                null,
                                null
                        )
                )
        );

        ReviewContext context = reviewContextBuildService.buildReviewContext(pullRequest);

        assertThat(context.taskId()).isEqualTo(1L);
        assertThat(context.prType()).isEqualTo(PrType.CODE);
        assertThat(context.prUrl()).isEqualTo("https://github.com/openai/pr-review-agent/pull/8");
        assertThat(context.parsedFileCount()).isEqualTo(2);
        assertThat(context.patchlessFileCount()).isEqualTo(1);
        assertThat(context.addedLineCount()).isEqualTo(4);
        assertThat(context.deletedLineCount()).isZero();
        assertThat(context.contextLineCount()).isZero();
        assertThat(context.changeScale()).isEqualTo("SMALL");
        assertThat(context.changedModules()).containsExactly("backend/src", "docs/架构设计.md");
        assertThat(context.files()).hasSize(2);
        assertThat(context.aiContext()).contains("PR Overview");
        assertThat(context.aiContext()).contains("PR Type: CODE");
        assertThat(context.aiContext()).contains("Change: ADDED, Category: BACKEND");
        assertThat(context.aiContext()).contains("Patch content unavailable from GitHub API");
        assertThat(context.aiContext()).doesNotContain("@@ -0,0 +1,4 @@");
    }
}
