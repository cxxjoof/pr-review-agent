package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.entity.ModelCallLog;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.repository.ModelCallLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelCallLogServiceTest {

    @Mock
    private ModelCallLogRepository modelCallLogRepository;

    @InjectMocks
    private ModelCallLogService modelCallLogService;

    @Test
    void shouldPersistSuccessfulModelCallLog() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);

        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(20);
        usage.setCompletionTokens(10);
        usage.setTotalTokens(30);

        modelCallLogService.recordSuccess(task, "SUMMARY", "gpt-4.1-mini", usage, 420L);

        ArgumentCaptor<ModelCallLog> captor = ArgumentCaptor.forClass(ModelCallLog.class);
        verify(modelCallLogRepository).save(captor.capture());

        ModelCallLog log = captor.getValue();
        assertThat(log.getTask()).isSameAs(task);
        assertThat(log.getCallType()).isEqualTo("SUMMARY");
        assertThat(log.getModelName()).isEqualTo("gpt-4.1-mini");
        assertThat(log.getPromptTokens()).isEqualTo(20);
        assertThat(log.getCompletionTokens()).isEqualTo(10);
        assertThat(log.getTotalTokens()).isEqualTo(30);
        assertThat(log.getSuccess()).isTrue();
        assertThat(log.getLatencyMs()).isEqualTo(420);
    }

    @Test
    void shouldPersistFailedModelCallLog() {
        ReviewTask task = new ReviewTask();
        task.setId(1L);

        modelCallLogService.recordFailure(task, "SUMMARY", "gpt-4.1-mini", " model api failed ", 520L);

        verify(modelCallLogRepository).save(any(ModelCallLog.class));
        ArgumentCaptor<ModelCallLog> captor = ArgumentCaptor.forClass(ModelCallLog.class);
        verify(modelCallLogRepository).save(captor.capture());

        ModelCallLog log = captor.getValue();
        assertThat(log.getTask()).isSameAs(task);
        assertThat(log.getCallType()).isEqualTo("SUMMARY");
        assertThat(log.getModelName()).isEqualTo("gpt-4.1-mini");
        assertThat(log.getSuccess()).isFalse();
        assertThat(log.getErrorMessage()).isEqualTo("model api failed");
        assertThat(log.getLatencyMs()).isEqualTo(520);
    }
}
