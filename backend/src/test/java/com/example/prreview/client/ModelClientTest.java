package com.example.prreview.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.prreview.config.ModelProperties;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.dto.model.ChatMessage;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.exception.ModelApiException;
import com.example.prreview.service.ModelCallLogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

class ModelClientTest {

    @Test
    void shouldCallChatCompletionAndRecordSuccessLog() {
        ModelCallLogService modelCallLogService = org.mockito.Mockito.mock(ModelCallLogService.class);
        ModelProperties modelProperties = new ModelProperties();
        modelProperties.setModelName("gpt-test");

        String responseBody = """
                {
                  "id": "chatcmpl-1",
                  "object": "chat.completion",
                  "created": 1710000000,
                  "model": "gpt-test",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Code review helps catch defects early."
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 12,
                    "completion_tokens": 8,
                    "total_tokens": 20
                  }
                }
                """;

        ModelClient modelClient = new ModelClient(
                createWebClient(request -> reactor.core.publisher.Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(responseBody)
                                .build()
                )),
                modelProperties,
                modelCallLogService
        );

        ChatCompletionResponse response = modelClient.chatCompletion(buildTask(), "SUMMARY", buildRequest());

        assertThat(response.getFirstMessageContent()).isEqualTo("Code review helps catch defects early.");
        verify(modelCallLogService).recordSuccess(
                any(ReviewTask.class),
                eq("SUMMARY"),
                eq("gpt-test"),
                any(ChatCompletionResponse.Usage.class),
                any(Long.class)
        );
    }

    @Test
    void shouldRejectInvalidModelResponseAndRecordFailureLog() {
        ModelCallLogService modelCallLogService = org.mockito.Mockito.mock(ModelCallLogService.class);
        ModelProperties modelProperties = new ModelProperties();
        modelProperties.setModelName("gpt-test");

        ModelClient modelClient = new ModelClient(
                createWebClient(request -> reactor.core.publisher.Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"choices\":[]}")
                                .build()
                )),
                modelProperties,
                modelCallLogService
        );

        assertThatThrownBy(() -> modelClient.chatCompletion(buildTask(), "SUMMARY", buildRequest()))
                .isInstanceOf(ModelApiException.class)
                .satisfies(throwable -> {
                    ModelApiException exception = (ModelApiException) throwable;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });

        verify(modelCallLogService).recordFailure(
                any(ReviewTask.class),
                eq("SUMMARY"),
                eq("gpt-test"),
                eq("Model API returned an invalid chat completion response."),
                any(Long.class)
        );
    }

    @Test
    void shouldMapUnauthorizedModelApiResponse() {
        ModelCallLogService modelCallLogService = org.mockito.Mockito.mock(ModelCallLogService.class);
        ModelProperties modelProperties = new ModelProperties();
        modelProperties.setModelName("gpt-test");

        ModelClient modelClient = new ModelClient(
                createWebClient(request -> reactor.core.publisher.Mono.just(
                        ClientResponse.create(HttpStatus.UNAUTHORIZED)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"error\":\"invalid_api_key\"}")
                                .build()
                )),
                modelProperties,
                modelCallLogService
        );

        assertThatThrownBy(() -> modelClient.chatCompletion(buildTask(), "SUMMARY", buildRequest()))
                .isInstanceOf(ModelApiException.class)
                .satisfies(throwable -> {
                    ModelApiException exception = (ModelApiException) throwable;
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("Model API key is invalid or missing required access.");
                });

        verify(modelCallLogService).recordFailure(
                any(ReviewTask.class),
                eq("SUMMARY"),
                eq("gpt-test"),
                eq("Model API key is invalid or missing required access."),
                any(Long.class)
        );
    }

    private WebClient createWebClient(ExchangeFunction exchangeFunction) {
        return WebClient.builder().exchangeFunction(exchangeFunction).build();
    }

    private ReviewTask buildTask() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);
        task.setRepoUrl("https://github.com/example/repo");
        task.setRepoOwner("example");
        task.setRepoName("repo");
        task.setPrNumber(1);
        return task;
    }

    private ChatCompletionRequest buildRequest() {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setMessages(List.of(new ChatMessage("user", "Explain why code review matters.")));
        request.setTemperature(0.2);
        return request;
    }
}
