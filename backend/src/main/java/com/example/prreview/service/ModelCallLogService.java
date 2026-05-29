package com.example.prreview.service;

import com.example.prreview.dto.model.ChatCompletionResponse;
import com.example.prreview.entity.ModelCallLog;
import com.example.prreview.entity.ReviewTask;
import com.example.prreview.repository.ModelCallLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ModelCallLogService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final ModelCallLogRepository modelCallLogRepository;

    public ModelCallLogService(ModelCallLogRepository modelCallLogRepository) {
        this.modelCallLogRepository = modelCallLogRepository;
    }

    public void recordSuccess(
            ReviewTask task,
            String callType,
            String modelName,
            ChatCompletionResponse.Usage usage,
            long latencyMs
    ) {
        ModelCallLog modelCallLog = new ModelCallLog();
        modelCallLog.setTask(task);
        modelCallLog.setCallType(callType);
        modelCallLog.setModelName(modelName);
        modelCallLog.setPromptTokens(usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens());
        modelCallLog.setCompletionTokens(
                usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens()
        );
        modelCallLog.setTotalTokens(usage == null || usage.getTotalTokens() == null ? 0 : usage.getTotalTokens());
        modelCallLog.setSuccess(Boolean.TRUE);
        modelCallLog.setLatencyMs(toInt(latencyMs));
        modelCallLogRepository.save(modelCallLog);
    }

    public void recordFailure(
            ReviewTask task,
            String callType,
            String modelName,
            String errorMessage,
            long latencyMs
    ) {
        ModelCallLog modelCallLog = new ModelCallLog();
        modelCallLog.setTask(task);
        modelCallLog.setCallType(callType);
        modelCallLog.setModelName(modelName);
        modelCallLog.setSuccess(Boolean.FALSE);
        modelCallLog.setErrorMessage(compactErrorMessage(errorMessage));
        modelCallLog.setLatencyMs(toInt(latencyMs));
        modelCallLogRepository.save(modelCallLog);
    }

    private int toInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private String compactErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return null;
        }

        String compacted = errorMessage.replaceAll("\\s+", " ").trim();
        return compacted.length() > MAX_ERROR_MESSAGE_LENGTH
                ? compacted.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "..."
                : compacted;
    }
}
