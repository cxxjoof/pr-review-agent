package com.example.prreview.service;

import com.example.prreview.dto.diff.ChangedFileContext;
import com.example.prreview.dto.diff.DiffLineDTO;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.enums.FileChangeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DiffParseService {

    private static final Pattern HUNK_HEADER_PATTERN =
            Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@.*$");

    public List<ChangedFileContext> parseChangedFiles(List<GitHubChangedFileDTO> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
                .map(this::parseChangedFile)
                .toList();
    }

    public ChangedFileContext parseChangedFile(GitHubChangedFileDTO file) {
        if (file == null) {
            throw new IllegalArgumentException("Changed file payload must not be null");
        }

        String patch = file.patch();
        if (patch == null || patch.isBlank()) {
            return new ChangedFileContext(
                    file.filename(),
                    file.previousFilename(),
                    resolveChangeType(file.status()),
                    detectFileCategory(file.filename()),
                    defaultZero(file.additions()),
                    defaultZero(file.deletions()),
                    defaultZero(file.changes()),
                    0,
                    0,
                    0,
                    0,
                    false,
                    "Patch content unavailable from GitHub API, likely binary or too large.",
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        List<DiffLineDTO> diffLines = new ArrayList<>();
        List<String> addedLines = new ArrayList<>();
        List<String> deletedLines = new ArrayList<>();
        int oldLine = 0;
        int newLine = 0;
        int hunkCount = 0;
        int contextLineCount = 0;
        int addedLineCount = 0;
        int deletedLineCount = 0;

        for (String line : splitPatchLines(patch)) {
            Matcher matcher = HUNK_HEADER_PATTERN.matcher(line);
            if (matcher.matches()) {
                oldLine = Integer.parseInt(matcher.group(1));
                newLine = Integer.parseInt(matcher.group(3));
                hunkCount++;
                continue;
            }

            if (line.startsWith("\\ No newline at end of file")) {
                continue;
            }

            if (line.startsWith("+")) {
                String content = line.substring(1);
                diffLines.add(new DiffLineDTO("ADDED", null, newLine, content));
                addedLines.add(content);
                newLine++;
                addedLineCount++;
                continue;
            }

            if (line.startsWith("-")) {
                String content = line.substring(1);
                diffLines.add(new DiffLineDTO("DELETED", oldLine, null, content));
                deletedLines.add(content);
                oldLine++;
                deletedLineCount++;
                continue;
            }

            if (line.startsWith(" ")) {
                String content = line.substring(1);
                diffLines.add(new DiffLineDTO("CONTEXT", oldLine, newLine, content));
                oldLine++;
                newLine++;
                contextLineCount++;
            }
        }

        return new ChangedFileContext(
                file.filename(),
                file.previousFilename(),
                resolveChangeType(file.status()),
                detectFileCategory(file.filename()),
                defaultZero(file.additions()),
                defaultZero(file.deletions()),
                defaultZero(file.changes()),
                hunkCount,
                addedLineCount,
                deletedLineCount,
                contextLineCount,
                true,
                null,
                List.copyOf(addedLines),
                List.copyOf(deletedLines),
                List.copyOf(diffLines)
        );
    }

    String detectFileCategory(String filePath) {
        String normalizedPath = filePath == null ? "" : filePath.trim();
        String lowercasePath = normalizedPath.toLowerCase(Locale.ROOT);

        if (lowercasePath.isBlank()) {
            return "OTHER";
        }

        if (lowercasePath.contains("/src/test/") || lowercasePath.contains("\\src\\test\\")
                || lowercasePath.contains("__tests__")
                || lowercasePath.endsWith("test.java")
                || lowercasePath.endsWith(".spec.js")
                || lowercasePath.endsWith(".test.js")
                || lowercasePath.endsWith(".spec.ts")
                || lowercasePath.endsWith(".test.ts")) {
            return "TEST";
        }

        if (lowercasePath.endsWith(".md") || lowercasePath.endsWith(".adoc") || lowercasePath.endsWith(".txt")) {
            return "DOCUMENTATION";
        }

        if (lowercasePath.endsWith(".sql")) {
            return "DATABASE";
        }

        if (lowercasePath.contains(".github/workflows/")
                || lowercasePath.endsWith(".yaml")
                || lowercasePath.endsWith(".yml")
                || lowercasePath.endsWith(".properties")
                || lowercasePath.endsWith(".env")
                || lowercasePath.endsWith("dockerfile")
                || lowercasePath.endsWith("docker-compose.yml")
                || lowercasePath.endsWith("docker-compose.yaml")) {
            return "CONFIG";
        }

        if (lowercasePath.endsWith("pom.xml")
                || lowercasePath.endsWith("package.json")
                || lowercasePath.endsWith("package-lock.json")
                || lowercasePath.endsWith("vite.config.js")
                || lowercasePath.endsWith("vite.config.ts")
                || lowercasePath.endsWith("build.gradle")
                || lowercasePath.endsWith("settings.gradle")) {
            return "BUILD";
        }

        if (lowercasePath.endsWith(".java") || lowercasePath.endsWith(".kt") || lowercasePath.endsWith(".groovy")) {
            return "BACKEND";
        }

        if (lowercasePath.endsWith(".js")
                || lowercasePath.endsWith(".jsx")
                || lowercasePath.endsWith(".ts")
                || lowercasePath.endsWith(".tsx")
                || lowercasePath.endsWith(".css")
                || lowercasePath.endsWith(".scss")
                || lowercasePath.endsWith(".html")
                || lowercasePath.endsWith(".vue")) {
            return "FRONTEND";
        }

        return "OTHER";
    }

    private FileChangeType resolveChangeType(String status) {
        if (status == null || status.isBlank()) {
            return FileChangeType.UNKNOWN;
        }

        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "added" -> FileChangeType.ADDED;
            case "modified", "changed" -> FileChangeType.MODIFIED;
            case "removed" -> FileChangeType.REMOVED;
            case "renamed" -> FileChangeType.RENAMED;
            case "copied" -> FileChangeType.COPIED;
            default -> FileChangeType.UNKNOWN;
        };
    }

    private List<String> splitPatchLines(String patch) {
        return List.of(patch.split("\\r?\\n"));
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}
