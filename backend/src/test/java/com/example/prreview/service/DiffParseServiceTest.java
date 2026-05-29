package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.dto.diff.ChangedFileContext;
import com.example.prreview.dto.diff.DiffLineDTO;
import com.example.prreview.dto.github.GitHubChangedFileDTO;
import com.example.prreview.enums.FileChangeType;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiffParseServiceTest {

    private final DiffParseService diffParseService = new DiffParseService();

    @Test
    void shouldParsePatchIntoStructuredDiffLines() {
        GitHubChangedFileDTO file = new GitHubChangedFileDTO(
                "backend/src/main/java/com/example/prreview/service/GitHubPrService.java",
                "modified",
                2,
                1,
                3,
                """
                @@ -10,3 +10,4 @@ public class GitHubPrService {
                  public String status() {
                -    return "old";
                +    return "new";
                +    // comment
                  }
                """,
                null
        );

        ChangedFileContext context = diffParseService.parseChangedFile(file);

        assertThat(context.changeType()).isEqualTo(FileChangeType.MODIFIED);
        assertThat(context.fileCategory()).isEqualTo("BACKEND");
        assertThat(context.patchAvailable()).isTrue();
        assertThat(context.hunkCount()).isEqualTo(1);
        assertThat(context.addedLineCount()).isEqualTo(2);
        assertThat(context.deletedLineCount()).isEqualTo(1);
        assertThat(context.contextLineCount()).isEqualTo(2);
        assertThat(context.addedLines()).containsExactly("    return \"new\";", "    // comment");
        assertThat(context.deletedLines()).containsExactly("    return \"old\";");

        List<DiffLineDTO> diffLines = context.diffLines();
        assertThat(diffLines).hasSize(5);
        assertThat(diffLines.get(0)).isEqualTo(new DiffLineDTO("CONTEXT", 10, 10, " public String status() {"));
        assertThat(diffLines.get(1)).isEqualTo(new DiffLineDTO("DELETED", 11, null, "    return \"old\";"));
        assertThat(diffLines.get(2)).isEqualTo(new DiffLineDTO("ADDED", null, 11, "    return \"new\";"));
        assertThat(diffLines.get(3)).isEqualTo(new DiffLineDTO("ADDED", null, 12, "    // comment"));
        assertThat(diffLines.get(4)).isEqualTo(new DiffLineDTO("CONTEXT", 12, 13, " }"));
    }

    @Test
    void shouldHandleMissingPatchGracefully() {
        GitHubChangedFileDTO file = new GitHubChangedFileDTO(
                "frontend/src/assets/logo.png",
                "added",
                0,
                0,
                0,
                null,
                null
        );

        ChangedFileContext context = diffParseService.parseChangedFile(file);

        assertThat(context.changeType()).isEqualTo(FileChangeType.ADDED);
        assertThat(context.fileCategory()).isEqualTo("OTHER");
        assertThat(context.patchAvailable()).isFalse();
        assertThat(context.patchNotice()).contains("Patch content unavailable");
        assertThat(context.diffLines()).isEmpty();
        assertThat(context.addedLines()).isEmpty();
        assertThat(context.deletedLines()).isEmpty();
    }

    @Test
    void shouldDetectCommonFileCategories() {
        assertThat(diffParseService.detectFileCategory("backend/src/test/java/com/example/AppTest.java"))
                .isEqualTo("TEST");
        assertThat(diffParseService.detectFileCategory("backend/src/main/resources/application.yml"))
                .isEqualTo("CONFIG");
        assertThat(diffParseService.detectFileCategory("docs/架构设计.md"))
                .isEqualTo("DOCUMENTATION");
        assertThat(diffParseService.detectFileCategory("frontend/src/pages/HomePage.jsx"))
                .isEqualTo("FRONTEND");
        assertThat(diffParseService.detectFileCategory("backend/pom.xml"))
                .isEqualTo("BUILD");
        assertThat(diffParseService.detectFileCategory("backend/src/main/resources/db/schema.sql"))
                .isEqualTo("DATABASE");
    }
}
