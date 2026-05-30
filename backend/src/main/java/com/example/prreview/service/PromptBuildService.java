package com.example.prreview.service;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatMessage;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptBuildService {

    private static final double REVIEW_TEMPERATURE = 0.1D;
    private static final int REVIEW_MAX_TOKENS = 2400;

    public ChatCompletionRequest buildReviewRequest(ReviewContext reviewContext) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setTemperature(REVIEW_TEMPERATURE);
        request.setMaxTokens(REVIEW_MAX_TOKENS);
        request.setResponseFormat(new ChatCompletionRequest.ResponseFormat("json_object"));
        request.setMessages(List.of(
                new ChatMessage("system", buildSystemPrompt()),
                new ChatMessage("user", buildUserPrompt(reviewContext))
        ));
        return request;
    }

    private String buildSystemPrompt() {
        return """
                你是一名资深 Pull Request 代码评审专家。
                你只能基于提供的 PR 上下文和 diff 摘要进行分析，不要臆造仓库外的信息。
                重点关注：正确性、异常处理、输入校验、安全性、性能、数据库影响、配置变更、测试缺失。
                当证据不足时，不要强行下结论，要在描述或建议中使用谨慎表述，例如“建议确认”。
                只返回合法 JSON，不要返回 Markdown 代码块，不要返回额外解释文字。

                所有自然语言字段必须使用简体中文输出，包括：
                - summary
                - changedModules 中的模块描述
                - riskItems 里的 description 和 suggestion
                - reviewSuggestions
                - testSuggestions
                - overallConclusion

                以下内容保持原样或使用约定值：
                - filePath 保持文件路径原样
                - codeSnippet 保持代码片段原样
                - riskLevel 只能是 HIGH、MEDIUM、LOW
                - riskType 只能是 NULL_POINTER、EXCEPTION_HANDLING、SECURITY、PERFORMANCE、INPUT_VALIDATION、DATABASE、CONFIG_CHANGE、TEST_MISSING、CODE_STYLE、OTHER

                必须严格使用下面这个 JSON 结构：
                {
                  "summary": "string",
                  "changedModules": ["string"],
                  "riskItems": [
                    {
                      "filePath": "string",
                      "lineNumber": 123,
                      "codeSnippet": "string or null",
                      "riskLevel": "HIGH|MEDIUM|LOW",
                      "riskType": "NULL_POINTER|EXCEPTION_HANDLING|SECURITY|PERFORMANCE|INPUT_VALIDATION|DATABASE|CONFIG_CHANGE|TEST_MISSING|CODE_STYLE|OTHER",
                      "description": "string",
                      "suggestion": "string",
                      "confidence": 0.85
                    }
                  ],
                  "reviewSuggestions": ["string"],
                  "testSuggestions": ["string"],
                  "overallConclusion": "string"
                }

                约束：
                - summary 必须简洁，并且只针对当前 PR。
                - riskItems 可以是空数组。
                - reviewSuggestions 和 testSuggestions 必须具体、可执行。
                - 只允许提到当前 PR 上下文中出现过的文件。
                """;
    }

    private String buildUserPrompt(ReviewContext reviewContext) {
        return """
                请分析下面的 GitHub Pull Request 上下文，并生成结构化代码评审报告。

                基本信息：
                - 仓库：%s/%s
                - PR 编号：%s
                - 标题：%s
                - 作者：%s
                - 分支：%s -> %s
                - 变更文件数：%s
                - 新增行数：%s
                - 删除行数：%s
                - 提交次数：%s
                - 成功解析的文件数：%s
                - 无 patch 文件数：%s
                - 变更规模：%s

                结构化 PR 上下文：
                %s
                """.formatted(
                defaultText(reviewContext.repoOwner()),
                defaultText(reviewContext.repoName()),
                defaultNumber(reviewContext.prNumber()),
                defaultText(reviewContext.title()),
                defaultText(reviewContext.author()),
                defaultText(reviewContext.sourceBranch()),
                defaultText(reviewContext.targetBranch()),
                defaultNumber(reviewContext.changedFiles()),
                defaultNumber(reviewContext.additions()),
                defaultNumber(reviewContext.deletions()),
                defaultNumber(reviewContext.commits()),
                defaultNumber(reviewContext.parsedFileCount()),
                defaultNumber(reviewContext.patchlessFileCount()),
                defaultText(reviewContext.changeScale()),
                defaultText(reviewContext.aiContext())
        );
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }
}
