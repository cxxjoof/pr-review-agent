package com.example.prreview.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class JsonParseUtils {

    private static final Pattern CODE_FENCE_PATTERN =
            Pattern.compile("```(?:json)?\\s*(\\{.*})\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private JsonParseUtils() {
    }

    public static String extractJsonObject(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            throw new IllegalArgumentException("JSON content must not be empty.");
        }

        String trimmed = rawContent.trim();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1).trim();
        }

        return trimmed;
    }

    public static <T> T readJsonObject(ObjectMapper objectMapper, String rawContent, Class<T> targetType)
            throws JsonProcessingException {
        return objectMapper.readValue(extractJsonObject(rawContent), targetType);
    }
}
