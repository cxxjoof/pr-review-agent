package com.example.prreview.validator;

import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class RepoUrlValidator {

    public static final String GITHUB_REPOSITORY_URL_REGEX =
            "^(https?://github\\.com/[^/]+/[^/]+?(?:\\.git)?/?|git@github\\.com:[^/]+/[^/]+?(?:\\.git)?)$";

    private static final Pattern GITHUB_REPOSITORY_PATTERN = Pattern.compile(GITHUB_REPOSITORY_URL_REGEX);

    private RepoUrlValidator() {
    }

    public static boolean isValid(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return false;
        }

        return GITHUB_REPOSITORY_PATTERN.matcher(repoUrl.trim()).matches();
    }
}
