package com.example.prreview.controller;

import com.example.prreview.dto.github.GitHubPullRequestDTO;
import com.example.prreview.service.GitHubPrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubPrService gitHubPrService;

    public GitHubController(GitHubPrService gitHubPrService) {
        this.gitHubPrService = gitHubPrService;
    }

    @GetMapping("/pr")
    public GitHubPullRequestDTO getPullRequest(
            @RequestParam String repoUrl,
            @RequestParam Integer prNumber
    ) {
        return gitHubPrService.fetchAndStorePullRequest(repoUrl, prNumber);
    }
}
