package com.example.prreview.client;

import com.example.prreview.common.ResultCode;
import com.example.prreview.config.GitHubProperties;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.exception.BusinessException;
import com.example.prreview.exception.GitHubApiException;
import com.example.prreview.validator.RepoUrlValidator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@Component
public class GitHubClient {

    private static final int PAGE_SIZE = 100;
    private static final Pattern HTTPS_REPOSITORY_PATTERN =
            Pattern.compile("^https?://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern SSH_REPOSITORY_PATTERN =
            Pattern.compile("^git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?$");

    private final WebClient gitHubWebClient;
    private final GitHubProperties gitHubProperties;

    public GitHubClient(WebClient gitHubWebClient, GitHubProperties gitHubProperties) {
        this.gitHubWebClient = gitHubWebClient;
        this.gitHubProperties = gitHubProperties;
    }

    public GitHubRepository parseRepository(String repoUrl) {
        String trimmedRepoUrl = repoUrl == null ? "" : repoUrl.trim();
        if (!RepoUrlValidator.isValid(trimmedRepoUrl)) {
            throw BusinessException.validation("repoUrl must be a valid GitHub repository URL.");
        }

        Matcher httpsMatcher = HTTPS_REPOSITORY_PATTERN.matcher(trimmedRepoUrl);
        if (httpsMatcher.matches()) {
            return new GitHubRepository(httpsMatcher.group(1), httpsMatcher.group(2));
        }

        Matcher sshMatcher = SSH_REPOSITORY_PATTERN.matcher(trimmedRepoUrl);
        if (sshMatcher.matches()) {
            return new GitHubRepository(sshMatcher.group(1), sshMatcher.group(2));
        }

        throw BusinessException.validation("repoUrl must be a valid GitHub repository URL.");
    }

    public GitHubPullRequestDTO getPullRequest(String repoUrl, GitHubRepository repository, Integer prNumber) {
        GitHubPullRequestResponse response = get(
                "/repos/%s/%s/pulls/%d".formatted(repository.owner(), repository.name(), prNumber),
                GitHubPullRequestResponse.class
        );

        List<GitHubChangedFileDTO> changedFiles = getPullRequestFiles(repository, prNumber, response.changedFiles());

        return new GitHubPullRequestDTO(
                null,
                response.id(),
                repoUrl,
                repository.owner(),
                repository.name(),
                prNumber,
                response.title(),
                response.body(),
                response.user() == null ? null : response.user().login(),
                response.head() == null ? null : response.head().ref(),
                response.base() == null ? null : response.base().ref(),
                response.state(),
                response.changedFiles(),
                response.additions(),
                response.deletions(),
                response.commits(),
                response.htmlUrl(),
                changedFiles
        );
    }

    public List<GitHubChangedFileDTO> getPullRequestFiles(
            GitHubRepository repository,
            Integer prNumber,
            Integer expectedChangedFiles
    ) {
        List<GitHubChangedFileDTO> files = new ArrayList<>();
        int page = 1;

        while (true) {
            List<GitHubChangedFileResponse> pageResponse = get(
                    "/repos/%s/%s/pulls/%d/files?per_page=%d&page=%d".formatted(
                            repository.owner(),
                            repository.name(),
                            prNumber,
                            PAGE_SIZE,
                            page
                    ),
                    new ParameterizedTypeReference<List<GitHubChangedFileResponse>>() {
                    }
            );

            if (pageResponse.isEmpty()) {
                break;
            }

            for (GitHubChangedFileResponse file : pageResponse) {
                files.add(new GitHubChangedFileDTO(
                        file.filename(),
                        file.status(),
                        file.additions(),
                        file.deletions(),
                        file.changes(),
                        file.patch(),
                        file.previousFilename()
                ));
            }

            if (pageResponse.size() < PAGE_SIZE || (expectedChangedFiles != null && files.size() >= expectedChangedFiles)) {
                break;
            }
            page++;
        }

        return files;
    }

    private <T> T get(String uri, Class<T> responseType) {
        try {
            return executeGet(uri).bodyToMono(responseType).block(requestTimeout());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw mapUnexpectedException(exception);
        }
    }

    private <T> T get(String uri, ParameterizedTypeReference<T> responseType) {
        try {
            return executeGet(uri).bodyToMono(responseType).block(requestTimeout());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw mapUnexpectedException(exception);
        }
    }

    private WebClient.ResponseSpec executeGet(String uri) {
        return gitHubWebClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> Mono.error(mapToGitHubException(
                                response.statusCode(),
                                response.headers().asHttpHeaders(),
                                body
                        ))));
    }

    private Duration requestTimeout() {
        return Duration.ofSeconds(gitHubProperties.getTimeoutSeconds());
    }

    private GitHubApiException mapToGitHubException(
            HttpStatusCode statusCode,
            HttpHeaders headers,
            String responseBody
    ) {
        if (statusCode.value() == HttpStatus.NOT_FOUND.value()) {
            return new GitHubApiException(
                    HttpStatus.NOT_FOUND,
                    ResultCode.NOT_FOUND,
                    "GitHub repository or pull request was not found. Check repoUrl and prNumber."
            );
        }

        if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            return new GitHubApiException(
                    HttpStatus.UNAUTHORIZED,
                    ResultCode.UNAUTHORIZED,
                    "GitHub token is invalid or missing required access."
            );
        }

        if (statusCode.value() == HttpStatus.FORBIDDEN.value() && "0".equals(headers.getFirst("X-RateLimit-Remaining"))) {
            return new GitHubApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ResultCode.RATE_LIMITED,
                    "GitHub API rate limit exceeded. Retry later or configure GITHUB_TOKEN."
            );
        }

        return new GitHubApiException(
                HttpStatus.BAD_GATEWAY,
                ResultCode.GITHUB_API_ERROR,
                "GitHub API request failed with HTTP status %s."
                        .formatted(statusCode.value())
        );
    }

    private GitHubApiException mapUnexpectedException(RuntimeException exception) {
        Throwable rootCause = Exceptions.unwrap(exception);
        if (rootCause instanceof TimeoutException) {
            return new GitHubApiException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    ResultCode.GATEWAY_TIMEOUT,
                    "GitHub API request timed out after %d seconds."
                            .formatted(gitHubProperties.getTimeoutSeconds())
            );
        }

        return new GitHubApiException(
                HttpStatus.BAD_GATEWAY,
                ResultCode.GITHUB_API_ERROR,
                "GitHub API request failed. Please retry later."
        );
    }

    public record GitHubRepository(String owner, String name) {
    }

    private record GitHubPullRequestResponse(
            Long id,
            String title,
            String body,
            GitHubUserResponse user,
            GitHubBranchResponse head,
            GitHubBranchResponse base,
            String state,
            @JsonProperty("changed_files") Integer changedFiles,
            Integer additions,
            Integer deletions,
            Integer commits,
            @JsonProperty("html_url") String htmlUrl
    ) {
    }

    private record GitHubChangedFileResponse(
            String filename,
            String status,
            Integer additions,
            Integer deletions,
            Integer changes,
            String patch,
            @JsonProperty("previous_filename") String previousFilename
    ) {
    }

    private record GitHubUserResponse(String login) {
    }

    private record GitHubBranchResponse(String ref) {
    }
}
