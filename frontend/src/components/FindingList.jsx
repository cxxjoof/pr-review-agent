import { DownOutlined, MessageOutlined, UpOutlined } from "@ant-design/icons";
import { Button, Card, Empty, Space, Tag, Typography } from "antd";
import { useState } from "react";
import {
  formatFeedbackStatus,
  formatFindingCategory,
  getFindingLevelMeta
} from "../utils/riskLevel.js";

const feedbackActions = [
  { label: "有用", value: "USEFUL" },
  { label: "误报", value: "FALSE_POSITIVE" },
  { label: "忽略", value: "IGNORED" },
  { label: "已修复", value: "FIXED" }
];

function renderConfidence(confidence) {
  if (confidence === null || confidence === undefined) {
    return "--";
  }

  const normalized = Number(confidence);
  if (Number.isNaN(normalized)) {
    return "--";
  }

  return `${Math.round(normalized * 100)}%`;
}

function CodeBlock({ value }) {
  if (!value) {
    return null;
  }

  return (
    <Typography.Paragraph className="code-snippet">
      {value}
    </Typography.Paragraph>
  );
}

function FindingList({
  title,
  findings = [],
  emptyDescription,
  onSubmitFeedback,
  submittingFeedback = {}
}) {
  const [expandedKeys, setExpandedKeys] = useState({});

  const toggleExpanded = (findingId) => {
    setExpandedKeys((current) => ({
      ...current,
      [findingId]: !current[findingId]
    }));
  };

  return (
    <Card className="result-card" bordered={false}>
      <Space direction="vertical" size={18} className="full-width">
        <div className="card-header">
          <div>
            <Typography.Title level={4} className="card-title">
              {title}
            </Typography.Title>
            <Typography.Paragraph className="card-subtitle">
              聚焦模型标出的高信号发现项，默认先展示最关键的摘要与建议。
            </Typography.Paragraph>
          </div>
        </div>

        {findings.length > 0 ? (
          <div className="finding-list">
            {findings.map((finding) => {
              const meta = getFindingLevelMeta(finding.findingLevel);
              const isExpanded = Boolean(expandedKeys[finding.id]);
              const loading = Boolean(submittingFeedback[finding.id]);

              return (
                <Card
                  key={finding.id ?? `${finding.filePath}-${finding.lineNumber}`}
                  className="finding-card"
                  bordered={false}
                >
                  <Space direction="vertical" size={14} className="full-width">
                    <div className="finding-card-header">
                      <Space wrap>
                        <Tag color={meta.color}>{meta.label}</Tag>
                        <Tag>{formatFindingCategory(finding.findingCategory)}</Tag>
                        <Tag>{finding.findingKind === "ADVISORY" ? "建议项" : "风险项"}</Tag>
                        {finding.feedbackStatus ? (
                          <Tag color="cyan">{formatFeedbackStatus(finding.feedbackStatus)}</Tag>
                        ) : null}
                      </Space>
                      <Typography.Text type="secondary">
                        置信度：{renderConfidence(finding.confidence)}
                      </Typography.Text>
                    </div>

                    <Space direction="vertical" size={6} className="full-width">
                      <Typography.Title level={5} className="finding-title">
                        {finding.title || "Review 发现项"}
                      </Typography.Title>
                      <Typography.Text strong>
                        {finding.filePath || "未知文件"}
                        {finding.lineNumber ? ` · 第 ${finding.lineNumber} 行` : ""}
                      </Typography.Text>
                      <Typography.Paragraph className="finding-summary">
                        {finding.suggestion || finding.description || "建议结合原始 diff 进行人工确认。"}
                      </Typography.Paragraph>
                    </Space>

                    <div className="finding-actions">
                      <Space wrap>
                        <Button
                          type="default"
                          icon={isExpanded ? <UpOutlined /> : <DownOutlined />}
                          onClick={() => toggleExpanded(finding.id)}
                        >
                          {isExpanded ? "收起详情" : "展开详情"}
                        </Button>
                        {finding.diffUrl ? (
                          <Button
                            type="link"
                            href={finding.diffUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            打开 GitHub Diff
                          </Button>
                        ) : null}
                      </Space>
                    </div>

                    {isExpanded ? (
                      <Space direction="vertical" size={14} className="full-width finding-detail">
                        <div>
                          <Typography.Text strong>问题描述</Typography.Text>
                          <Typography.Paragraph className="detail-paragraph">
                            {finding.description || "模型未返回更详细的问题描述。"}
                          </Typography.Paragraph>
                        </div>

                        {finding.codeSnippet ? (
                          <div>
                            <Typography.Text strong>代码片段</Typography.Text>
                            <CodeBlock value={finding.codeSnippet} />
                          </div>
                        ) : null}

                        {finding.beforeExample ? (
                          <div>
                            <Typography.Text strong>修改前</Typography.Text>
                            <CodeBlock value={finding.beforeExample} />
                          </div>
                        ) : null}

                        {finding.afterExample ? (
                          <div>
                            <Typography.Text strong>修改后</Typography.Text>
                            <CodeBlock value={finding.afterExample} />
                          </div>
                        ) : null}

                        {finding.suggestedPatch ? (
                          <div>
                            <Typography.Text strong>建议补丁</Typography.Text>
                            <CodeBlock value={finding.suggestedPatch} />
                          </div>
                        ) : null}

                        <div>
                          <Typography.Text strong>反馈</Typography.Text>
                          <div className="feedback-action-group">
                            {feedbackActions.map((action) => (
                              <Button
                                key={action.value}
                                icon={<MessageOutlined />}
                                loading={loading}
                                disabled={loading}
                                onClick={() => onSubmitFeedback?.(finding.id, action.value)}
                              >
                                {action.label}
                              </Button>
                            ))}
                          </div>
                        </div>
                      </Space>
                    ) : null}
                  </Space>
                </Card>
              );
            })}
          </div>
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={emptyDescription}
          />
        )}
      </Space>
    </Card>
  );
}

export default FindingList;
