package com.example.prreview.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.util.StringUtils;

public final class GitHubDiffUrlBuilder {

    private GitHubDiffUrlBuilder() {
    }

    public static String build(String prUrl, String filePath, Integer lineNumber) {
        if (!StringUtils.hasText(prUrl)) {
            return null;
        }
        if (!StringUtils.hasText(filePath)) {
            return prUrl + "/files";
        }

        String normalizedPath = filePath.replace("\\", "/").trim();
        String anchor = "diff-" + sha256Hex(normalizedPath);
        if (lineNumber != null && lineNumber > 0) {
            anchor += "R" + lineNumber;
        }
        return prUrl + "/files#" + anchor;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte element : hash) {
                builder.append(String.format("%02x", element));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable.", exception);
        }
    }
}
