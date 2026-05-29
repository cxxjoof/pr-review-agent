package com.example.prreview.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubConfig {

    @Bean
    WebClient gitHubWebClient(WebClient.Builder webClientBuilder, GitHubProperties gitHubProperties) {
        WebClient.Builder builder = webClientBuilder
                .baseUrl(gitHubProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "pr-review-agent")
                .defaultHeader("X-GitHub-Api-Version", gitHubProperties.getApiVersion());

        if (StringUtils.hasText(gitHubProperties.getToken())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubProperties.getToken().trim());
        }

        return builder.build();
    }
}
