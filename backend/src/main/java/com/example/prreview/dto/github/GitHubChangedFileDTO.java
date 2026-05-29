package com.example.prreview.dto.github;

public record GitHubChangedFileDTO(
        String filename,
        String status,
        Integer additions,
        Integer deletions,
        Integer changes,
        String patch,
        String previousFilename
) {
}
