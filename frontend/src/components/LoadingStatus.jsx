import { Card, Progress, Space, Spin, Steps, Typography } from "antd";

const loadingStepItems = [
  {
    title: "提交分析任务",
    description: "向后端发送仓库地址和 PR 编号。"
  },
  {
    title: "等待 AI Review",
    description: "后端正在抓取 PR、解析 diff 并调用模型。"
  },
  {
    title: "读取结果报告",
    description: "整理完整的 PR 审查结果用于展示。"
  }
];

function LoadingStatus({ currentStep = 1, repoUrl, prNumber }) {
  const percent = Math.min(Math.max(Math.round((currentStep / 3) * 100), 20), 95);

  return (
    <Card className="form-card loading-card" bordered={false}>
      <Space direction="vertical" size={24} className="full-width">
        <div className="loading-heading">
          <Spin size="large" />
          <div>
            <Typography.Title level={3} className="form-title">
              正在生成 AI Review 报告
            </Typography.Title>
            <Typography.Paragraph className="form-description">
              当前正在分析 `{repoUrl}` 的 PR #{prNumber}。根据变更规模不同，这一步可能持续几十秒。
            </Typography.Paragraph>
          </div>
        </div>

        <Progress percent={percent} showInfo={false} strokeColor="#136f63" />

        <Steps
          direction="vertical"
          current={Math.max(currentStep - 1, 0)}
          items={loadingStepItems}
        />
      </Space>
    </Card>
  );
}

export default LoadingStatus;
