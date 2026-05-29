package com.example.prreview.client;

import com.example.prreview.config.ModelProperties;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.service.ModelCallLogService;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@Component
public class ModelClient {

    private static final String DEFAULT_CALL_TYPE = "CHAT_COMPLETION";

    private final WebClient modelWebClient;
    private final ModelProperties modelProperties;
    private final ModelCallLogService modelCallLogService;

    public ModelClient(
            WebClient modelWebClient,
            ModelProperties modelProperties,
            ModelCallLogService modelCallLogService
    ) {
        this.modelWebClient = modelWebClient;
        this.modelProperties = modelProperties;
        this.modelCallLogService = modelCallLogService;
    }

    public ChatCompletionResponse chatCompletion(
            ReviewTask task,
            String callType,
            ChatCompletionRequest request
    ) {
        validateTask(task);
        ChatCompletionRequest actualRequest = prepareRequest(request);
        String actualCallType = StringUtils.hasText(callType) ? callType.trim() : DEFAULT_CALL_TYPE;
        long startTime = System.nanoTime();

        try {
            ChatCompletionResponse response = modelWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(actualRequest)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(mapToModelException(clientResponse.statusCode(), body))))
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(requestTimeout())
                    .block();

            validateResponse(response);
            modelCallLogService.recordSuccess(
                    task,
                    actualCallType,
                    StringUtils.hasText(response.getModel()) ? response.getModel() : actualRequest.getModel(),
                    response.getUsage(),
                    elapsedMillis(startTime)
            );
            return response;
        } catch (ResponseStatusException exception) {
            modelCallLogService.recordFailure(
                    task,
                    actualCallType,
                    actualRequest.getModel(),
                    exception.getReason(),
                    elapsedMillis(startTime)
            );
            throw exception;
        } catch (RuntimeException exception) {
            ResponseStatusException mappedException = mapUnexpectedException(exception);
            modelCallLogService.recordFailure(
                    task,
                    actualCallType,
                    actualRequest.getModel(),
                    mappedException.getReason(),
                    elapsedMillis(startTime)
            );
            throw mappedException;
        }
    }

    private void validateTask(ReviewTask task) {
        if (task == null || task.getId() == null) {
            throw new IllegalArgumentException("ReviewTask with persisted id is required to record model call logs.");
        }
    }

    private ChatCompletionRequest prepareRequest(ChatCompletionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat completion request must not be null.");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chat completion request must include at least one message."
            );
        }

        if (!StringUtils.hasText(request.getModel())) {
            if (!StringUtils.hasText(modelProperties.getModelName())) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Model name is not configured. Set MODEL_API_MODEL before calling the model API."
                );
            }
            request.setModel(modelProperties.getModelName().trim());
        }

        return request;
    }

    private void validateResponse(ChatCompletionResponse response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Model API returned an empty response.");
        }
        if (!StringUtils.hasText(response.getFirstMessageContent())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Model API returned an invalid chat completion response."
            );
        }
    }

    private Duration requestTimeout() {
        return Duration.ofSeconds(modelProperties.getTimeoutSeconds());
    }

    private ResponseStatusException mapToModelException(HttpStatusCode statusCode, String responseBody) {
        if (statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
            return new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Model API key is invalid or missing required access."
            );
        }

        if (statusCode.value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Model API rate limit exceeded. Retry later or use another model provider configuration."
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Model API request failed. HTTP status: %s, response: %s"
                        .formatted(statusCode.value(), compactBody(responseBody))
        );
    }

    private ResponseStatusException mapUnexpectedException(RuntimeException exception) {
        Throwable rootCause = Exceptions.unwrap(exception);
        if (rootCause instanceof TimeoutException) {
            return new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Model API request timed out after %d seconds."
                            .formatted(modelProperties.getTimeoutSeconds())
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Model API request failed: " + compactBody(rootCause == null ? exception.getMessage() : rootCause.getMessage())
        );
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    private String compactBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "empty";
        }

        String compacted = body.replaceAll("\\s+", " ").trim();
        return compacted.length() > 200 ? compacted.substring(0, 200) + "..." : compacted;
    }
}
