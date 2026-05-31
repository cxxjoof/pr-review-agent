import { Card, Descriptions, Space, Tag, Typography } from "antd";

function ReviewSummaryCard({ reviewDetail }) {
  const summary = reviewDetail?.reviewResult?.summary;
  const changedModules = reviewDetail?.reviewResult?.changedModules ?? [];
  const overallConclusion = reviewDetail?.reviewResult?.overallConclusion;
  const findingCount = reviewDetail?.findingCount ?? 0;
  const status = reviewDetail?.status ?? "UNKNOWN";
  const prType = reviewDetail?.prType ?? "UNKNOWN";
  const resultViewType = reviewDetail?.resultViewType ?? "REVIEW_FINDINGS";

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
          <Space wrap>
            <Tag color="processing" className="metric-tag">
              状态：{status}
            </Tag>
            <Tag color="default" className="metric-tag">
              PR 类型：{prType}
            </Tag>
            <Tag color="default" className="metric-tag">
              发现项：{findingCount}
            </Tag>
          </Space>
        </div>

        <Descriptions column={1} size="small" className="summary-descriptions">
          <Descriptions.Item label="变更摘要">
            {summary || "模型暂未返回变更摘要。"}
          </Descriptions.Item>
          <Descriptions.Item label="结果视图">
            {resultViewType}
          </Descriptions.Item>
          <Descriptions.Item label="总体结论">
            {overallConclusion || "模型暂未返回总体结论。"}
          </Descriptions.Item>
        </Descriptions>

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
