package com.example.prreview.dto.diff;

import com.example.prreview.enums.PrType;
import java.util.List;

public record ReviewContext(
        Long taskId,
        String repoUrl,
        String repoOwner,
        String repoName,
        Integer prNumber,
        String prUrl,
        PrType prType,
        String title,
        String description,
        String author,
        String sourceBranch,
        String targetBranch,
        Integer changedFiles,
        Integer additions,
        Integer deletions,
        Integer commits,
        List<String> changedModules,
        Integer parsedFileCount,
        Integer patchlessFileCount,
        Integer addedLineCount,
        Integer deletedLineCount,
        Integer contextLineCount,
        String changeScale,
        List<ChangedFileContext> files,
        String aiContext
) {
}
