import { Card, Progress, Space, Spin, Steps, Typography } from "antd";

const loadingStepItems = [
  {
    title: "正在提交分析任务...",
    description: "向后端发送仓库地址和 PR 编号。"
  },
  {
    title: "正在获取 PR 信息并分析代码变更...",
    description: "当前为前端分阶段展示，实际耗时会随 PR 规模变化。"
  },
  {
    title: "正在生成 Review 报告并读取结果...",
    description: "系统会在报告准备完成后自动切换到结果页。"
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
              当前正在分析 {repoUrl} 的 PR #{prNumber}。根据变更规模不同，这一步可能持续几十秒。
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
