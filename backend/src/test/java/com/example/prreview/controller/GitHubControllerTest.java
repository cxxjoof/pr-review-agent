package com.example.prreview.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prreview.client.GitHubClient;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.entity.PullRequestInfo;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.PullRequestInfoRepository;
import com.example.prreview.repository.ReviewTaskRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Autowired
    private PullRequestInfoRepository pullRequestInfoRepository;

    @MockBean
    private GitHubClient gitHubClient;

    @BeforeEach
    void setUp() {
        pullRequestInfoRepository.deleteAll();
        reviewTaskRepository.deleteAll();
    }

    @Test
    void shouldFetchAndPersistPullRequestInfo() throws Exception {
        GitHubClient.GitHubRepository repository = new GitHubClient.GitHubRepository("openai", "pr-review-agent");
        GitHubPullRequestDTO pullRequest = new GitHubPullRequestDTO(
                null,
                101L,
                "https://github.com/openai/pr-review-agent",
                "openai",
                "pr-review-agent",
                8,
                "feat: add github pr fetch module",
                "Add GitHub API integration",
                "octocat",
                "feature/github-pr",
                "main",
                "open",
                2,
                20,
                5,
                1,
                "https://github.com/openai/pr-review-agent/pull/8",
                List.of(new GitHubChangedFileDTO(
                        "backend/src/main/java/com/example/prreview/client/GitHubClient.java",
                        "added",
                        20,
                        0,
                        20,
                        "@@ -0,0 +1,20 @@",
                        null
                ))
        );

        given(gitHubClient.parseRepository("https://github.com/openai/pr-review-agent")).willReturn(repository);
        given(gitHubClient.getPullRequest("https://github.com/openai/pr-review-agent", repository, 8))
                .willReturn(pullRequest);

        mockMvc.perform(get("/api/github/pr")
                        .param("repoUrl", "https://github.com/openai/pr-review-agent")
                        .param("prNumber", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").isNumber())
                .andExpect(jsonPath("$.repoOwner").value("openai"))
                .andExpect(jsonPath("$.repoName").value("pr-review-agent"))
                .andExpect(jsonPath("$.title").value("feat: add github pr fetch module"))
                .andExpect(jsonPath("$.files[0].filename")
                        .value("backend/src/main/java/com/example/prreview/client/GitHubClient.java"));

        List<ReviewTask> tasks = reviewTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        ReviewTask task = tasks.get(0);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.SUCCESS);
        assertThat(task.getRepoOwner()).isEqualTo("openai");
        assertThat(task.getRepoName()).isEqualTo("pr-review-agent");

        PullRequestInfo pullRequestInfo = pullRequestInfoRepository.findByTaskId(task.getId()).orElseThrow();
        assertThat(pullRequestInfo.getGithubPrId()).isEqualTo(101L);
        assertThat(pullRequestInfo.getAuthor()).isEqualTo("octocat");
        assertThat(pullRequestInfo.getChangedFiles()).isEqualTo(2);
    }

    @Test
    void shouldMarkTaskAsFailedWhenGitHubFetchFails() throws Exception {
        GitHubClient.GitHubRepository repository = new GitHubClient.GitHubRepository("openai", "pr-review-agent");

        given(gitHubClient.parseRepository("https://github.com/openai/pr-review-agent")).willReturn(repository);
        given(gitHubClient.getPullRequest(eq("https://github.com/openai/pr-review-agent"), eq(repository), eq(99)))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "PR not found"));

        mockMvc.perform(get("/api/github/pr")
                        .param("repoUrl", "https://github.com/openai/pr-review-agent")
                        .param("prNumber", "99"))
                .andExpect(status().isNotFound());

        List<ReviewTask> tasks = reviewTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        ReviewTask task = tasks.get(0);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).contains("404 NOT_FOUND");
    }
}
