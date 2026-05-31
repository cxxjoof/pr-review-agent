package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.enums.PrType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromptBuildServiceTest {

    private final PromptBuildService promptBuildService = new PromptBuildService();

    @Test
    void shouldBuildJsonReviewRequestFromReviewContext() {
        ReviewContext reviewContext = new ReviewContext(
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
                3,
                25,
                7,
                1,
                List.of("backend/service", "backend/dto"),
                3,
                0,
                20,
                5,
                8,
                "SMALL",
                List.of(),
                "PR Overview\nChanged Files\n- File: backend/src/main/java/com/example/prreview/service/AiReviewService.java"
        );

        ChatCompletionRequest request = promptBuildService.buildReviewRequest(reviewContext);

        assertThat(request.getTemperature()).isEqualTo(0.1D);
        assertThat(request.getMaxTokens()).isEqualTo(2800);
        assertThat(request.getResponseFormat()).isNotNull();
        assertThat(request.getResponseFormat().getType()).isEqualTo("json_object");
        assertThat(request.getMessages()).hasSize(2);
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("system");
        assertThat(request.getMessages().get(0).getContent()).contains("JSON");
        assertThat(request.getMessages().get(0).getContent()).contains("findings");
        assertThat(request.getMessages().get(0).getContent()).contains("代码型 PR");
        assertThat(request.getMessages().get(1).getRole()).isEqualTo("user");
        assertThat(request.getMessages().get(1).getContent()).contains("openai");
        assertThat(request.getMessages().get(1).getContent()).contains("pr-review-agent");
        assertThat(request.getMessages().get(1).getContent()).contains("AiReviewService.java");
        assertThat(request.getMessages().get(1).getContent()).contains("CODE");
    }
}
