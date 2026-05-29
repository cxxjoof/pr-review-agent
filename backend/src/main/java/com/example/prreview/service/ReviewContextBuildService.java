package com.example.prreview.service;

import com.example.prreview.dto.diff.ChangedFileContext;
import com.example.prreview.dto.diff.DiffLineDTO;
import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.github.GitHubPullRequestDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReviewContextBuildService {

    private static final int MAX_DIFF_LINES_PER_FILE = 20;

    private final DiffParseService diffParseService;

    public ReviewContextBuildService(DiffParseService diffParseService) {
        this.diffParseService = diffParseService;
    }

    public ReviewContext buildReviewContext(GitHubPullRequestDTO pullRequest) {
        if (pullRequest == null) {
            throw new IllegalArgumentException("Pull request payload must not be null");
        }

        List<ChangedFileContext> fileContexts = diffParseService.parseChangedFiles(pullRequest.files());
        int patchlessFileCount = (int) fileContexts.stream()
                .filter(file -> !file.patchAvailable())
                .count();
        int addedLineCount = fileContexts.stream()
                .map(ChangedFileContext::addedLineCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int deletedLineCount = fileContexts.stream()
                .map(ChangedFileContext::deletedLineCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int contextLineCount = fileContexts.stream()
                .map(ChangedFileContext::contextLineCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        List<String> changedModules = fileContexts.stream()
                .map(ChangedFileContext::filePath)
                .map(this::extractModuleName)
                .distinct()
                .toList();

        return new ReviewContext(
                pullRequest.taskId(),
                pullRequest.repoUrl(),
                pullRequest.repoOwner(),
                pullRequest.repoName(),
                pullRequest.prNumber(),
                pullRequest.title(),
                pullRequest.description(),
                pullRequest.author(),
                pullRequest.sourceBranch(),
                pullRequest.targetBranch(),
                pullRequest.changedFiles(),
                pullRequest.additions(),
                pullRequest.deletions(),
                pullRequest.commits(),
                changedModules,
                fileContexts.size(),
                patchlessFileCount,
                addedLineCount,
                deletedLineCount,
                contextLineCount,
                determineChangeScale(fileContexts.size(), defaultZero(pullRequest.additions()) + defaultZero(pullRequest.deletions())),
                fileContexts,
                buildAiContext(pullRequest, fileContexts, changedModules, patchlessFileCount)
        );
    }

    private String buildAiContext(
            GitHubPullRequestDTO pullRequest,
            List<ChangedFileContext> fileContexts,
            List<String> changedModules,
            int patchlessFileCount
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("PR Overview").append(System.lineSeparator());
        builder.append("Title: ").append(defaultString(pullRequest.title())).append(System.lineSeparator());
        builder.append("Repository: ").append(defaultString(pullRequest.repoOwner()))
                .append("/")
                .append(defaultString(pullRequest.repoName()))
                .append(System.lineSeparator());
        builder.append("PR Number: ").append(defaultZero(pullRequest.prNumber())).append(System.lineSeparator());
        builder.append("Author: ").append(defaultString(pullRequest.author())).append(System.lineSeparator());
        builder.append("Branches: ").append(defaultString(pullRequest.sourceBranch()))
                .append(" -> ")
                .append(defaultString(pullRequest.targetBranch()))
                .append(System.lineSeparator());
        builder.append("Changed files: ").append(defaultZero(pullRequest.changedFiles()))
                .append(", additions: +").append(defaultZero(pullRequest.additions()))
                .append(", deletions: -").append(defaultZero(pullRequest.deletions()))
                .append(", commits: ").append(defaultZero(pullRequest.commits()))
                .append(System.lineSeparator());
        builder.append("Changed modules: ").append(String.join(", ", changedModules)).append(System.lineSeparator());
        builder.append("Files without patch: ").append(patchlessFileCount).append(System.lineSeparator());

        String description = defaultString(pullRequest.description());
        if (!description.isBlank()) {
            builder.append("Description: ").append(description).append(System.lineSeparator());
        }

        builder.append(System.lineSeparator()).append("Changed Files").append(System.lineSeparator());

        List<ChangedFileContext> prioritizedFiles = fileContexts.stream()
                .sorted(Comparator.comparing(ChangedFileContext::changes, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        for (ChangedFileContext file : prioritizedFiles) {
            builder.append("- File: ").append(file.filePath()).append(System.lineSeparator());
            builder.append("  Change: ").append(file.changeType())
                    .append(", Category: ").append(file.fileCategory())
                    .append(", Stats: +").append(defaultZero(file.additions()))
                    .append("/-").append(defaultZero(file.deletions()))
                    .append(", Hunks: ").append(defaultZero(file.hunkCount()))
                    .append(System.lineSeparator());

            if (!file.patchAvailable()) {
                builder.append("  Patch: ").append(defaultString(file.patchNotice())).append(System.lineSeparator());
                continue;
            }

            List<DiffLineDTO> excerpt = file.diffLines().stream()
                    .limit(MAX_DIFF_LINES_PER_FILE)
                    .toList();
            builder.append("  Diff excerpt:").append(System.lineSeparator());
            for (DiffLineDTO line : excerpt) {
                builder.append("    [").append(line.lineType()).append("] ")
                        .append(formatLineNumbers(line.oldLineNumber(), line.newLineNumber()))
                        .append(" ")
                        .append(line.content())
                        .append(System.lineSeparator());
            }

            if (file.diffLines().size() > MAX_DIFF_LINES_PER_FILE) {
                builder.append("    ... truncated ")
                        .append(file.diffLines().size() - MAX_DIFF_LINES_PER_FILE)
                        .append(" more diff lines")
                        .append(System.lineSeparator());
            }
        }

        return builder.toString().trim();
    }

    private String extractModuleName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "unknown";
        }

        String normalized = filePath.replace("\\", "/");
        String[] segments = normalized.split("/");
        if (segments.length == 0) {
            return normalized;
        }

        if (segments.length > 1 && ("backend".equals(segments[0]) || "frontend".equals(segments[0]) || "docs".equals(segments[0]))) {
            return segments[0] + "/" + segments[1];
        }

        return segments[0];
    }

    private String determineChangeScale(int fileCount, int totalChangedLines) {
        if (fileCount <= 3 && totalChangedLines <= 120) {
            return "SMALL";
        }

        if (fileCount <= 10 && totalChangedLines <= 500) {
            return "MEDIUM";
        }

        return "LARGE";
    }

    private String formatLineNumbers(Integer oldLineNumber, Integer newLineNumber) {
        return "%s -> %s".formatted(
                oldLineNumber == null ? "-" : oldLineNumber,
                newLineNumber == null ? "-" : newLineNumber
        );
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
