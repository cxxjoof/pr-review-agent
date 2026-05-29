package com.example.prreview.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.prreview.config.GitHubProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

class GitHubClientTest {

    private final GitHubClient gitHubClient = new GitHubClient(WebClient.builder().build(), new GitHubProperties());

    @Test
    void shouldParseHttpsRepositoryUrl() {
        GitHubClient.GitHubRepository repository =
                gitHubClient.parseRepository("https://github.com/openai/pr-review-agent");

        assertThat(repository.owner()).isEqualTo("openai");
        assertThat(repository.name()).isEqualTo("pr-review-agent");
    }

    @Test
    void shouldParseSshRepositoryUrl() {
        GitHubClient.GitHubRepository repository =
                gitHubClient.parseRepository("git@github.com:openai/pr-review-agent.git");

        assertThat(repository.owner()).isEqualTo("openai");
        assertThat(repository.name()).isEqualTo("pr-review-agent");
    }

    @Test
    void shouldRejectInvalidRepositoryUrl() {
        assertThatThrownBy(() -> gitHubClient.parseRepository("https://gitlab.com/openai/pr-review-agent"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(throwable -> {
                    ResponseStatusException exception = (ResponseStatusException) throwable;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }
}
