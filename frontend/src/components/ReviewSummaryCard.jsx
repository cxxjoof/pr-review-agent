import { Card, Descriptions, Space, Tag, Typography } from "antd";
import { formatPrType } from "../utils/riskLevel.js";

function ReviewSummaryCard({ reviewDetail }) {
  const summary = reviewDetail?.reviewResult?.summary;
  const changedModules = reviewDetail?.reviewResult?.changedModules ?? [];
  const overallConclusion = reviewDetail?.reviewResult?.overallConclusion;
  const findingCount = reviewDetail?.findingCount ?? reviewDetail?.findings?.length ?? 0;
  const status = reviewDetail?.status ?? "UNKNOWN";
  const prType = reviewDetail?.prType ?? "UNKNOWN";

  return (
    <Card className="summary-card" bordered={false}>
      <Space direction="vertical" size={18} className="full-width">
        <div className="card-header">
          <div>
            <Typography.Title level={4} className="card-title">
              Review 摘要
            </Typography.Title>
            <Typography.Paragraph className="card-subtitle">
              汇总本次 PR 的主要改动、影响模块和整体评审判断。
            </Typography.Paragraph>
          </div>
        </div>

        <div className="summary-highlight-grid">
          <div className="summary-highlight-item">
            <Typography.Text type="secondary">PR 类型</Typography.Text>
            <Typography.Title level={4}>{formatPrType(prType)}</Typography.Title>
          </div>
          <div className="summary-highlight-item">
            <Typography.Text type="secondary">发现项数量</Typography.Text>
            <Typography.Title level={4}>{findingCount}</Typography.Title>
          </div>
          <div className="summary-highlight-item">
            <Typography.Text type="secondary">任务状态</Typography.Text>
            <Typography.Title level={4}>{status}</Typography.Title>
          </div>
        </div>

        <Descriptions column={1} size="small" className="summary-descriptions">
          <Descriptions.Item label="变更摘要">
            {summary || "模型暂未返回变更摘要。"}
          </Descriptions.Item>
        </Descriptions>

        <div className="summary-conclusion">
          <Tag color="processing" className="metric-tag">
            总体结论
          </Tag>
          <Typography.Paragraph className="summary-conclusion-text">
            {overallConclusion || "模型暂未返回总体结论。"}
          </Typography.Paragraph>
        </div>

        <div>
          <Typography.Text strong>涉及模块</Typography.Text>
          <div className="module-tag-group">
            {changedModules.length > 0 ? (
              changedModules.map((moduleName) => (
                <Tag key={moduleName} className="module-tag">
                  {moduleName}
                </Tag>
              ))
            ) : (
              <Typography.Paragraph className="empty-state">
                当前结果没有给出明确的变更模块划分。
              </Typography.Paragraph>
            )}
          </div>
        </div>
      </Space>
    </Card>
  );
}

export default ReviewSummaryCard;
