package com.example.prreview.dto.diff;

import com.example.prreview.enums.FileChangeType;
import java.util.List;

public record ChangedFileContext(
        String filePath,
        String previousFilePath,
        FileChangeType changeType,
        String fileCategory,
        Integer additions,
        Integer deletions,
        Integer changes,
        Integer hunkCount,
        Integer addedLineCount,
        Integer deletedLineCount,
        Integer contextLineCount,
        boolean patchAvailable,
        String patchNotice,
        List<String> addedLines,
        List<String> deletedLines,
        List<DiffLineDTO> diffLines
) {
}
