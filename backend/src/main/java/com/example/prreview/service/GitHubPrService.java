package com.example.prreview.service;

import com.example.prreview.client.GitHubClient;
import com.example.prreview.client.GitHubClient.GitHubRepository;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.entity.PullRequestInfo;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.PullRequestInfoRepository;
import com.example.prreview.repository.ReviewTaskRepository;
import org.springframework.stereotype.Service;

@Service
public class GitHubPrService {

    private final GitHubClient gitHubClient;
    private final ReviewTaskRepository reviewTaskRepository;
    private final PullRequestInfoRepository pullRequestInfoRepository;

    public GitHubPrService(
            GitHubClient gitHubClient,
            ReviewTaskRepository reviewTaskRepository,
            PullRequestInfoRepository pullRequestInfoRepository
    ) {
        this.gitHubClient = gitHubClient;
        this.reviewTaskRepository = reviewTaskRepository;
        this.pullRequestInfoRepository = pullRequestInfoRepository;
    }

    public GitHubPullRequestDTO fetchAndStorePullRequest(String repoUrl, Integer prNumber) {
        GitHubRepository repository = gitHubClient.parseRepository(repoUrl);
        ReviewTask task = createRunningTask(repoUrl, repository, prNumber);

        try {
            GitHubPullRequestDTO pullRequest = gitHubClient.getPullRequest(repoUrl, repository, prNumber);
            savePullRequestInfo(task, pullRequest);

            task.setStatus(TaskStatus.SUCCESS);
            task.setErrorMessage(null);
            ReviewTask savedTask = reviewTaskRepository.save(task);

            return new GitHubPullRequestDTO(
                    savedTask.getId(),
                    pullRequest.githubPrId(),
                    pullRequest.repoUrl(),
                    pullRequest.repoOwner(),
                    pullRequest.repoName(),
                    pullRequest.prNumber(),
                    pullRequest.title(),
                    pullRequest.description(),
                    pullRequest.author(),
                    pullRequest.sourceBranch(),
                    pullRequest.targetBranch(),
                    pullRequest.state(),
                    pullRequest.changedFiles(),
                    pullRequest.additions(),
                    pullRequest.deletions(),
                    pullRequest.commits(),
                    pullRequest.prUrl(),
                    pullRequest.files()
            );
        } catch (RuntimeException exception) {
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(exception.getMessage());
            reviewTaskRepository.save(task);
            throw exception;
        }
    }

    private ReviewTask createRunningTask(String repoUrl, GitHubRepository repository, Integer prNumber) {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl(repoUrl);
        task.setRepoOwner(repository.owner());
        task.setRepoName(repository.name());
        task.setPrNumber(prNumber);
        task.setStatus(TaskStatus.RUNNING);
        return reviewTaskRepository.save(task);
    }

    private void savePullRequestInfo(ReviewTask task, GitHubPullRequestDTO pullRequest) {
        PullRequestInfo pullRequestInfo = new PullRequestInfo();
        pullRequestInfo.setTask(task);
        pullRequestInfo.setGithubPrId(pullRequest.githubPrId());
        pullRequestInfo.setTitle(pullRequest.title());
        pullRequestInfo.setDescription(pullRequest.description());
        pullRequestInfo.setAuthor(pullRequest.author());
        pullRequestInfo.setSourceBranch(pullRequest.sourceBranch());
        pullRequestInfo.setTargetBranch(pullRequest.targetBranch());
        pullRequestInfo.setState(pullRequest.state());
        pullRequestInfo.setChangedFiles(pullRequest.changedFiles());
        pullRequestInfo.setAdditions(pullRequest.additions());
        pullRequestInfo.setDeletions(pullRequest.deletions());
        pullRequestInfo.setCommits(pullRequest.commits());
        pullRequestInfo.setPrUrl(pullRequest.prUrl());
        pullRequestInfoRepository.save(pullRequestInfo);
    }
}
