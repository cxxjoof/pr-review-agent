import { startTransition, useState } from "react";
import {
  Alert,
  Card,
  Col,
  message,
  Row,
  Space,
  Statistic,
  Tag,
  Typography
} from "antd";
import PrInputForm from "../components/PrInputForm.jsx";
import LoadingStatus from "../components/LoadingStatus.jsx";
import ReviewResultPage from "./ReviewResultPage.jsx";
import {
  createReviewTask,
  getReviewResult,
  submitFindingFeedback
} from "../api/reviewApi.js";

const featureTags = [
  "PR 类型识别",
  "高信号发现项",
  "结构化 Review",
  "修复示例",
  "反馈闭环"
];

function HomePage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [submitting, setSubmitting] = useState(false);
  const [loadingStep, setLoadingStep] = useState(1);
  const [submitError, setSubmitError] = useState("");
  const [lastSubmission, setLastSubmission] = useState(null);
  const [reviewDetail, setReviewDetail] = useState(null);
  const [submittingFeedback, setSubmittingFeedback] = useState({});

  const handleSubmit = async (values) => {
    setSubmitting(true);
    setSubmitError("");
    setReviewDetail(null);
    setLastSubmission(values);
    setLoadingStep(1);

    try {
      setLoadingStep(2);
      const task = await createReviewTask(values);
      setLoadingStep(3);

      const detail = await getReviewResult(task.taskId);
      startTransition(() => {
        setReviewDetail(detail);
      });
      messageApi.success("AI Review 报告已生成。");
    } catch (error) {
      setSubmitError(error.message || "分析失败，请稍后重试。");
      messageApi.error("分析任务执行失败。");
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitFeedback = async (findingId, feedbackType) => {
    if (!reviewDetail?.taskId || !findingId) {
      return;
    }

    setSubmittingFeedback((current) => ({
      ...current,
      [findingId]: true
    }));

    try {
      const feedback = await submitFindingFeedback(reviewDetail.taskId, findingId, {
        feedbackType
      });

      startTransition(() => {
        setReviewDetail((current) => {
          if (!current) {
            return current;
          }

          return {
            ...current,
            findings: (current.findings ?? []).map((finding) =>
              finding.id === findingId
                ? { ...finding, feedbackStatus: feedback.feedbackType }
                : finding
            )
          };
        });
      });
      messageApi.success("反馈已记录。");
    } catch (error) {
      messageApi.error(error.message || "提交反馈失败。");
    } finally {
      setSubmittingFeedback((current) => ({
        ...current,
        [findingId]: false
      }));
    }
  };

  const handleReset = () => {
    setReviewDetail(null);
    setSubmitError("");
    setSubmitting(false);
    setLoadingStep(1);
    setSubmittingFeedback({});
  };

  if (reviewDetail) {
    return (
      <>
        {contextHolder}
        <ReviewResultPage
          reviewDetail={reviewDetail}
          submittingFeedback={submittingFeedback}
          onSubmitFeedback={handleSubmitFeedback}
          onBack={handleReset}
          onAnalyzeAnother={handleReset}
        />
      </>
    );
  }

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
                  AI PR Review
                </Tag>
                <Space direction="vertical" size={12}>
                  <Typography.Title level={1} className="hero-title">
                    AI PR Review 助手
                  </Typography.Title>
                  <Typography.Paragraph className="hero-description">
                    输入 GitHub 仓库地址和 Pull Request 编号，系统会先识别 PR 类型，再生成结构化发现项、
                    Review 建议、测试建议和可执行修复线索。
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
                  <Typography.Title level={4}>当前优化重点</Typography.Title>
                  <Typography.Paragraph>
                    当前版本重点解决文档类 PR 误报偏重、风险类型过粗、建议不可直接采纳、结果页横向滚动重，
                    并增加反馈闭环能力。
                  </Typography.Paragraph>
                </Card>
              </Space>
            </Col>
            <Col xs={24} lg={11}>
              {submitting && lastSubmission ? (
                <LoadingStatus
                  currentStep={loadingStep}
                  repoUrl={lastSubmission.repoUrl}
                  prNumber={lastSubmission.prNumber}
                />
              ) : (
                <Space direction="vertical" size={16} className="full-width">
                  {submitError ? (
                    <Alert
                      type="error"
                      showIcon
                      message="分析失败"
                      description={submitError}
                    />
                  ) : null}
                  <PrInputForm loading={submitting} onSubmit={handleSubmit} />
                </Space>
              )}
            </Col>
          </Row>

          <Row gutter={[24, 24]} className="status-row">
            <Col xs={24} md={12}>
              <Card className="status-card" bordered={false}>
                <Typography.Title level={4}>当前支持能力</Typography.Title>
                <Space direction="vertical" size={10}>
                  <Alert message="PR 类型识别" type="success" showIcon />
                  <Alert message="结构化发现项输出" type="success" showIcon />
                  <Alert message="修改前 / 修改后示例展示" type="success" showIcon />
                  <Alert message="反馈闭环接口" type="success" showIcon />
                </Space>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card className="status-card" bordered={false}>
                <Typography.Title level={4}>联调状态</Typography.Title>
                {lastSubmission ? (
                  <Space direction="vertical" size={16} className="full-width">
                    <Statistic
                      title="最近一次 PR 编号"
                      value={lastSubmission.prNumber}
                      className="result-statistic"
                    />
                    <Typography.Text>
                      最近一次仓库地址：{lastSubmission.repoUrl}
                    </Typography.Text>
                    <Typography.Paragraph className="empty-state">
                      提交成功后会自动读取详细报告，并切换到完整的结果展示页。
                    </Typography.Paragraph>
                  </Space>
                ) : (
                  <Typography.Paragraph className="empty-state">
                    还没有提交表单。你可以先输入示例仓库地址，体验真实的前后端联调流程。
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
