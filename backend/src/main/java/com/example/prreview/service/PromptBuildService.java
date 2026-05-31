package com.example.prreview.service;

import com.example.prreview.dto.diff.ReviewContext;
import com.example.prreview.dto.model.ChatCompletionRequest;
import com.example.prreview.dto.model.ChatMessage;
import com.example.prreview.enums.PrType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptBuildService {

    private static final double REVIEW_TEMPERATURE = 0.1D;
    private static final int REVIEW_MAX_TOKENS = 2800;

    public ChatCompletionRequest buildReviewRequest(ReviewContext reviewContext) {
        if (reviewContext == null) {
            throw new IllegalArgumentException("Review context must not be null.");
        }

        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setTemperature(REVIEW_TEMPERATURE);
        request.setMaxTokens(REVIEW_MAX_TOKENS);
        request.setResponseFormat(new ChatCompletionRequest.ResponseFormat("json_object"));
        request.setMessages(List.of(
                new ChatMessage("system", buildSystemPrompt(reviewContext.prType())),
                new ChatMessage("user", buildUserPrompt(reviewContext))
        ));
        return request;
    }

    private String buildSystemPrompt(PrType prType) {
        return """
                你是一名资深 Pull Request Reviewer。
                你只能基于提供的 PR 上下文和 diff 摘要进行分析，不要臆造仓库外信息。
                你的目标是输出高信号、可验证、可修复的问题，不要为了“多找问题”而制造噪声评论。
                当证据不足时，必须使用 ADVISORY，描述中使用“建议确认”，不要直接下高风险结论。
                只返回合法 JSON，不要返回 Markdown 代码块，不要返回额外解释文字。

                所有自然语言字段必须使用简体中文输出，包括：
                - summary
                - changedModules
                - findings 里的 title、description、suggestion、beforeExample、afterExample、suggestedPatch
                - reviewSuggestions
                - testSuggestions
                - overallConclusion

                以下内容保持原样或使用约定值：
                - filePath 保持文件路径原样
                - codeSnippet 保持代码片段原样
                - findingLevel 只能是 HIGH、MEDIUM、LOW、ADVISORY
                - findingKind 只能是 RISK、ADVISORY
                - findingCategory 只能是 DOCUMENTATION_FORMAT、COMMAND_EXECUTABILITY、PATH_COMPATIBILITY、TEST_GAP、CODE_LOGIC、SECURITY、EXCEPTION_HANDLING、PERFORMANCE、MAINTAINABILITY、CONFIG_COMPATIBILITY、DEPENDENCY_CHANGE、CICD_CHANGE、OTHER

                输出必须严格使用下面的 JSON 结构：
                {
                  "summary": "string",
                  "changedModules": ["string"],
                  "findings": [
                    {
                      "filePath": "string",
                      "lineNumber": 123,
                      "codeSnippet": "string or null",
                      "findingLevel": "HIGH|MEDIUM|LOW|ADVISORY",
                      "findingKind": "RISK|ADVISORY",
                      "findingCategory": "DOCUMENTATION_FORMAT|COMMAND_EXECUTABILITY|PATH_COMPATIBILITY|TEST_GAP|CODE_LOGIC|SECURITY|EXCEPTION_HANDLING|PERFORMANCE|MAINTAINABILITY|CONFIG_COMPATIBILITY|DEPENDENCY_CHANGE|CICD_CHANGE|OTHER",
                      "title": "string",
                      "description": "string",
                      "suggestion": "string",
                      "beforeExample": "string or null",
                      "afterExample": "string or null",
                      "suggestedPatch": "string or null",
                      "confidence": 0.85
                    }
                  ],
                  "reviewSuggestions": ["string"],
                  "testSuggestions": ["string"],
                  "overallConclusion": "string"
                }

                约束：
                - summary 必须简洁，只针对当前 PR。
                - findings 可以是空数组。
                - reviewSuggestions 和 testSuggestions 必须具体、可执行。
                - 只允许提到当前 PR 上下文中出现过的文件。
                - 文档、格式、表达、可维护性问题默认只能输出 LOW 或 ADVISORY，不得输出 MEDIUM/HIGH。
                - 无明确证据时不要输出 HIGH。

                当前 PR 类型策略：
                %s
                """.formatted(buildStrategyPrompt(prType));
    }

    private String buildStrategyPrompt(PrType prType) {
        if (prType == null) {
            return "未知类型 PR：保持保守判断，只输出证据充分的问题。";
        }

        return switch (prType) {
            case DOCUMENTATION -> """
                    这是文档型 PR。重点检查 Markdown 格式、命令可执行性、路径兼容性、说明是否清晰、是否容易误导新用户。
                    不要把纯文档问题描述为“代码风险”，除非文档错误会直接导致错误操作。大多数问题应为 LOW 或 ADVISORY。
                    尽量给出 beforeExample / afterExample / suggestedPatch。
                    """;
            case CODE -> """
                    这是代码型 PR。重点检查逻辑 bug、异常处理、输入校验、安全、性能和测试缺口。
                    只有在变更证据明确时才输出 HIGH 或 MEDIUM。
                    """;
            case CONFIG -> """
                    这是配置型 PR。重点检查兼容性、环境变量、端口、开关、默认值和部署影响。
                    对潜在影响但证据不足的问题优先使用 ADVISORY。
                    """;
            case TEST -> """
                    这是测试型 PR。重点检查测试覆盖是否真实有效、断言是否可靠、是否遗漏关键回归场景。
                    对非功能性建议优先使用 LOW 或 ADVISORY。
                    """;
            case DEPENDENCY -> """
                    这是依赖变更型 PR。重点检查版本兼容性、潜在 breaking change、安全风险和验证建议。
                    如果没有明确变更影响证据，使用 ADVISORY。
                    """;
            case CICD -> """
                    这是 CI/CD 型 PR。重点检查流水线步骤、缓存、环境变量、权限和部署稳定性。
                    对推测性问题使用 ADVISORY。
                    """;
            case MIXED -> """
                    这是混合型 PR。请按文件和变更类型分别判断，不要把文档问题和代码逻辑风险混在一起描述。
                    文档问题使用 LOW 或 ADVISORY；代码问题按证据判断等级。
                    """;
        };
    }

    private String buildUserPrompt(ReviewContext reviewContext) {
        return """
                请分析下面的 GitHub Pull Request 上下文，并生成结构化代码审查报告。
                基本信息：
                - 仓库：%s/%s
                - PR 编号：%s
                - PR 类型：%s
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
                reviewContext.prType() == null ? "UNKNOWN" : reviewContext.prType().name(),
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
