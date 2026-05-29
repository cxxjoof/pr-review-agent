package com.example.prreview.dto.diff;

public record DiffLineDTO(
        String lineType,
        Integer oldLineNumber,
        Integer newLineNumber,
        String content
) {
}
