import { ArrowLeftOutlined, LinkOutlined, ReloadOutlined } from "@ant-design/icons";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Row,
  Space,
  Tag,
  Typography
} from "antd";
import ReviewSummaryCard from "../components/ReviewSummaryCard.jsx";
import RiskItemTable from "../components/RiskItemTable.jsx";
import SuggestionList from "../components/SuggestionList.jsx";
import { getRiskLevelMeta } from "../utils/riskLevel.js";

function formatDateTime(value) {
  if (!value) {
    return "未提供";
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(parsedDate);
}

function ReviewResultPage({ reviewDetail, onBack, onAnalyzeAnother }) {
  const pullRequest = reviewDetail?.pullRequest ?? {};
  const reviewResult = reviewDetail?.reviewResult ?? {};
  const riskItems = reviewDetail?.riskItems ?? [];
  const highestRiskLevel = riskItems.find((item) => item.riskLevel)?.riskLevel;
  const highestRiskMeta = highestRiskLevel
    ? getRiskLevelMeta(highestRiskLevel)
    : { label: "暂无高风险", color: "success" };

  return (
    <div className="app-shell">
      <div className="page-glow page-glow-left" />
      <div className="page-glow page-glow-right" />
      <div className="page-content">
        <Space direction="vertical" size={24} className="full-width">
          <div className="result-topbar">
            <Button icon={<ArrowLeftOutlined />} onClick={onBack}>
              返回重新输入
            </Button>
            <Button type="primary" ghost icon={<ReloadOutlined />} onClick={onAnalyzeAnother}>
              分析新的 PR
            </Button>
          </div>

          <Card className="result-hero-card" bordered={false}>
            <Row gutter={[24, 24]} align="middle">
              <Col xs={24} lg={15}>
                <Space direction="vertical" size={14}>
                  <Tag className="hero-tag" bordered={false}>
                    Review Result
                  </Tag>
                  <Typography.Title level={1} className="result-title">
                    {pullRequest.title || `${pullRequest.repoOwner}/${pullRequest.repoName} PR #${pullRequest.prNumber}`}
                  </Typography.Title>
                  <Typography.Paragraph className="hero-description">
                    {reviewResult.summary || "报告已生成，你可以从风险项、Review 建议和测试建议三个维度快速复查这次改动。"}
                  </Typography.Paragraph>
                  <Space wrap>
                    <Tag className="feature-tag">{pullRequest.repoOwner}/{pullRequest.repoName}</Tag>
                    <Tag className="feature-tag">PR #{pullRequest.prNumber}</Tag>
                    <Tag className="feature-tag">状态：{reviewDetail?.status}</Tag>
                    <Tag color={highestRiskMeta.color}>{highestRiskMeta.label}</Tag>
                  </Space>
                </Space>
              </Col>
              <Col xs={24} lg={9}>
                <Descriptions column={1} size="small" className="hero-metrics">
                  <Descriptions.Item label="作者">{pullRequest.author || "未提供"}</Descriptions.Item>
                  <Descriptions.Item label="分支">
                    {pullRequest.sourceBranch || "unknown"} → {pullRequest.targetBranch || "unknown"}
                  </Descriptions.Item>
                  <Descriptions.Item label="文件变更">
                    {pullRequest.changedFiles ?? 0} 个文件 / +{pullRequest.additions ?? 0} / -{pullRequest.deletions ?? 0}
                  </Descriptions.Item>
                  <Descriptions.Item label="生成时间">
                    {formatDateTime(reviewDetail?.updatedAt)}
                  </Descriptions.Item>
                </Descriptions>
                {pullRequest.prUrl ? (
                  <>
                    <Divider className="result-divider" />
                    <Button
                      type="link"
                      icon={<LinkOutlined />}
                      href={pullRequest.prUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="link-button"
                    >
                      打开原始 Pull Request
                    </Button>
                  </>
                ) : null}
              </Col>
            </Row>
          </Card>

          {reviewDetail?.errorMessage ? (
            <Alert
              type="warning"
              showIcon
              message="任务返回了错误信息"
              description={reviewDetail.errorMessage}
            />
          ) : null}

          <Row gutter={[24, 24]}>
            <Col xs={24} xl={14}>
              <ReviewSummaryCard reviewDetail={reviewDetail} />
            </Col>
            <Col xs={24} xl={10}>
              <Card className="result-card" bordered={false}>
                <Space direction="vertical" size={18} className="full-width">
                  <div className="card-header">
                    <div>
                      <Typography.Title level={4} className="card-title">
                        PR 基本信息
                      </Typography.Title>
                      <Typography.Paragraph className="card-subtitle">
                        用于快速确认本次分析对应的仓库、分支和变更规模。
                      </Typography.Paragraph>
                    </div>
                  </div>

                  <Descriptions column={1} size="small" className="summary-descriptions">
                    <Descriptions.Item label="仓库地址">
                      {pullRequest.repoUrl || "未提供"}
                    </Descriptions.Item>
                    <Descriptions.Item label="PR 描述">
                      {pullRequest.description || "暂无描述"}
                    </Descriptions.Item>
                    <Descriptions.Item label="提交次数">
                      {pullRequest.commits ?? 0}
                    </Descriptions.Item>
                    <Descriptions.Item label="PR 状态">
                      {pullRequest.state || "未提供"}
                    </Descriptions.Item>
                  </Descriptions>
                </Space>
              </Card>
            </Col>
          </Row>

          <RiskItemTable riskItems={riskItems} />

          <Row gutter={[24, 24]}>
            <Col xs={24} xl={12}>
              <SuggestionList
                title="Review 建议"
                description="这些建议更偏向代码实现、鲁棒性和可维护性。"
                items={reviewResult.reviewSuggestions}
                variant="review"
              />
            </Col>
            <Col xs={24} xl={12}>
              <SuggestionList
                title="测试建议"
                description="这些建议帮助你补齐回归验证、边界场景和接口覆盖。"
                items={reviewResult.testSuggestions}
                variant="test"
              />
            </Col>
          </Row>
        </Space>
      </div>
    </div>
  );
}

export default ReviewResultPage;
