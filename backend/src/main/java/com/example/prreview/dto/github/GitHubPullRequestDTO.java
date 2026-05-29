package com.example.prreview.dto.github;

import java.util.List;

public record GitHubPullRequestDTO(
        Long taskId,
        Long githubPrId,
        String repoUrl,
        String repoOwner,
        String repoName,
        Integer prNumber,
        String title,
        String description,
        String author,
        String sourceBranch,
        String targetBranch,
        String state,
        Integer changedFiles,
        Integer additions,
        Integer deletions,
        Integer commits,
        String prUrl,
        List<GitHubChangedFileDTO> files
) {
}
