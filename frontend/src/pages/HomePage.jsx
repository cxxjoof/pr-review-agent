import { useState } from "react";
import { Alert, Card, Col, message, Row, Space, Tag, Typography } from "antd";
import PrInputForm from "../components/PrInputForm.jsx";

const featureTags = [
  "PR 变更总结",
  "风险代码识别",
  "Review 建议",
  "测试建议"
];

function HomePage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [submitting, setSubmitting] = useState(false);
  const [lastSubmission, setLastSubmission] = useState(null);

  const handleSubmit = async (values) => {
    setSubmitting(true);

    await new Promise((resolve) => {
      window.setTimeout(resolve, 900);
    });

    setLastSubmission(values);
    setSubmitting(false);
    messageApi.success("前端基础页面已就绪，后续模块会在这里接入真实分析流程。");
  };

  return (
    <>
      {contextHolder}
      <div className="app-shell">
        <div className="page-glow page-glow-left" />
        <div className="page-glow page-glow-right" />
        <div className="page-content">
          <Row gutter={[24, 24]} align="middle">
            <Col xs={24} lg={13}>
              <Space direction="vertical" size={24} className="hero-section">
                <Tag className="hero-tag" bordered={false}>
                  Frontend Base Page
                </Tag>
                <Space direction="vertical" size={12}>
                  <Typography.Title level={1} className="hero-title">
                    AI PR Review 助手
                  </Typography.Title>
                  <Typography.Paragraph className="hero-description">
                    输入 GitHub 仓库地址和 Pull Request 编号，后续模块会基于
                    PR diff 生成结构化代码审查报告。当前页面先完成基础输入、
                    布局和交互反馈，为后端联调预留好位置。
                  </Typography.Paragraph>
                </Space>
                <div className="feature-tag-group">
                  {featureTags.map((tag) => (
                    <Tag key={tag} className="feature-tag">
                      {tag}
                    </Tag>
                  ))}
                </div>
                <Card className="info-card" bordered={false}>
                  <Typography.Title level={4}>当前模块范围</Typography.Title>
                  <Typography.Paragraph>
                    本阶段仅实现前端基础首页和 PR 输入表单，不调用后端接口，
                    不展示最终 Review 报告。
                  </Typography.Paragraph>
                </Card>
              </Space>
            </Col>
            <Col xs={24} lg={11}>
              <PrInputForm loading={submitting} onSubmit={handleSubmit} />
            </Col>
          </Row>

          <Row gutter={[24, 24]} className="status-row">
            <Col xs={24} md={12}>
              <Card className="status-card" bordered={false}>
                <Typography.Title level={4}>页面验收点</Typography.Title>
                <Space direction="vertical" size={10}>
                  <Alert message="系统标题可见" type="success" showIcon />
                  <Alert message="GitHub 仓库地址输入框已提供" type="success" showIcon />
                  <Alert message="PR 编号输入框已提供" type="success" showIcon />
                  <Alert message="开始分析按钮已提供" type="success" showIcon />
                </Space>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card className="status-card" bordered={false}>
                <Typography.Title level={4}>最近一次表单输入</Typography.Title>
                {lastSubmission ? (
                  <Space direction="vertical" size={8}>
                    <Typography.Text>
                      仓库地址：{lastSubmission.repoUrl}
                    </Typography.Text>
                    <Typography.Text>
                      PR 编号：{lastSubmission.prNumber}
                    </Typography.Text>
                  </Space>
                ) : (
                  <Typography.Paragraph className="empty-state">
                    还没有提交表单。你可以先输入示例仓库地址，体验基础交互和加载状态。
                  </Typography.Paragraph>
                )}
              </Card>
            </Col>
          </Row>
        </div>
      </div>
    </>
  );
}

export default HomePage;
