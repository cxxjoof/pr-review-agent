import {
  CheckCircleOutlined,
  CodeOutlined,
  FileSearchOutlined,
  RadarChartOutlined,
  ToolOutlined
} from "@ant-design/icons";
import { startTransition, useEffect, useRef, useState } from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  message,
  Row,
  Space,
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

const capabilityItems = [
  {
    key: "pr-type",
    icon: <RadarChartOutlined />,
    title: "PR 类型识别",
    description: "自动判断是功能开发、Bug 修复、文档修改、测试补充还是重构类 PR。"
  },
  {
    key: "high-signal",
    icon: <FileSearchOutlined />,
    title: "高信号发现项",
    description: "优先输出真正可能影响质量的问题，减少泛泛而谈。"
  },
  {
    key: "structured-review",
    icon: <CodeOutlined />,
    title: "结构化 Review",
    description: "按风险等级、文件位置、问题原因、修改建议组织结果。"
  },
  {
    key: "fix-feedback",
    icon: <ToolOutlined />,
    title: "修复示例与反馈闭环",
    description: "支持修改前后示例、建议补丁和结果反馈。"
  }
];

const sampleReportPreview = {
  prType: "功能开发",
  riskLevel: "中",
  findings: 3,
  testSuggestions: 2,
  fixSuggestions: 1
};

function formatAnalysisStatus(status) {
  if (status === "ANALYZING") {
    return { label: "分析中", color: "processing" };
  }

  if (status === "SUCCESS") {
    return { label: "分析成功", color: "success" };
  }

  if (status === "FAILED") {
    return { label: "分析失败", color: "error" };
  }

  return { label: "未开始", color: "default" };
}

function formatDuration(durationMs) {
  if (!durationMs && durationMs !== 0) {
    return "--";
  }

  const seconds = durationMs / 1000;
  return `${seconds >= 10 ? seconds.toFixed(0) : seconds.toFixed(1)}s`;
}

function formatRepoDisplay(repoUrl) {
  if (!repoUrl) {
    return "--";
  }

  return repoUrl
    .replace(/^https?:\/\/github\.com\//i, "")
    .replace(/^git@github\.com:/i, "")
    .replace(/\.git$/i, "");
}

function HomePage() {
  const [messageApi, contextHolder] = message.useMessage();
  const [submitting, setSubmitting] = useState(false);
  const [loadingStep, setLoadingStep] = useState(1);
  const [submitError, setSubmitError] = useState("");
  const [recentAnalysis, setRecentAnalysis] = useState(null);
  const [reviewDetail, setReviewDetail] = useState(null);
  const [latestCompletedReview, setLatestCompletedReview] = useState(null);
  const [submittingFeedback, setSubmittingFeedback] = useState({});
  const loadingStepTimerRef = useRef(null);

  useEffect(() => () => {
    if (loadingStepTimerRef.current) {
      window.clearTimeout(loadingStepTimerRef.current);
    }
  }, []);

  const clearLoadingStepTimer = () => {
    if (loadingStepTimerRef.current) {
      window.clearTimeout(loadingStepTimerRef.current);
      loadingStepTimerRef.current = null;
    }
  };

  const applyFeedbackToDetail = (detail, findingId, feedbackType) => {
    if (!detail) {
      return detail;
    }

    return {
      ...detail,
      findings: (detail.findings ?? []).map((finding) =>
        finding.id === findingId
          ? { ...finding, feedbackStatus: feedbackType }
          : finding
      )
    };
  };

  const handleSubmit = async (values) => {
    const startedAt = Date.now();
    let createdTask = null;

    setSubmitting(true);
    setSubmitError("");
    setReviewDetail(null);
    setLoadingStep(1);
    setRecentAnalysis({
      repoUrl: values.repoUrl,
      prNumber: values.prNumber,
      status: "ANALYZING",
      startedAt,
      finishedAt: null,
      durationMs: null,
      taskId: null,
      errorMessage: ""
    });
    clearLoadingStepTimer();
    loadingStepTimerRef.current = window.setTimeout(() => {
      setLoadingStep(2);
    }, 700);

    try {
      createdTask = await createReviewTask(values);
      clearLoadingStepTimer();
      setLoadingStep(3);

      const detail = await getReviewResult(createdTask.taskId);
      const finishedAt = Date.now();
      const durationMs = finishedAt - startedAt;
      startTransition(() => {
        setLatestCompletedReview(detail);
        setReviewDetail(detail);
        setRecentAnalysis({
          repoUrl: values.repoUrl,
          prNumber: values.prNumber,
          status: "SUCCESS",
          startedAt,
          finishedAt,
          durationMs,
          taskId: createdTask.taskId,
          errorMessage: ""
        });
      });
      messageApi.success("分析完成，正在展示报告。");
    } catch (error) {
      clearLoadingStepTimer();
      const finishedAt = Date.now();
      const errorMessage = error?.message?.trim() || "GitHub API 调用失败，请稍后重试。";

      setSubmitError(errorMessage);
      setRecentAnalysis({
        repoUrl: values.repoUrl,
        prNumber: values.prNumber,
        status: "FAILED",
        startedAt,
        finishedAt,
        durationMs: finishedAt - startedAt,
        taskId: createdTask?.taskId ?? null,
        errorMessage
      });
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
        setReviewDetail((current) =>
          applyFeedbackToDetail(current, findingId, feedback.feedbackType)
        );
        setLatestCompletedReview((current) =>
          applyFeedbackToDetail(current, findingId, feedback.feedbackType)
        );
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
    clearLoadingStepTimer();
  };

  const handleViewLatestReport = () => {
    if (!latestCompletedReview) {
      return;
    }

    setSubmitError("");
    setReviewDetail(latestCompletedReview);
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
                    输入 GitHub 仓库地址和 Pull Request 编号，系统会自动识别 PR 类型，分析代码变更风险，
                    并生成结构化 Review 报告、测试建议和修复示例。
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
                    当前版本聚焦公开 GitHub PR 的快速分析体验，重点突出 PR 类型识别、高信号发现项、
                    结构化报告和可执行修复线索，适合本地演示与前后端联调验证。
                  </Typography.Paragraph>
                </Card>
              </Space>
            </Col>
            <Col xs={24} lg={11}>
              {submitting && recentAnalysis ? (
                <LoadingStatus
                  currentStep={loadingStep}
                  repoUrl={recentAnalysis.repoUrl}
                  prNumber={recentAnalysis.prNumber}
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
                <div className="capability-grid">
                  {capabilityItems.map((item) => (
                    <div key={item.key} className="capability-item">
                      <div className="capability-icon">{item.icon}</div>
                      <div>
                        <Typography.Title level={5} className="capability-title">
                          {item.title}
                        </Typography.Title>
                        <Typography.Paragraph className="capability-description">
                          {item.description}
                        </Typography.Paragraph>
                      </div>
                    </div>
                  ))}
                </div>
              </Card>
            </Col>
            <Col xs={24} md={12}>
              <Card className="status-card" bordered={false}>
                <Typography.Title level={4}>最近一次分析记录</Typography.Title>
                {recentAnalysis ? (
                  <Space direction="vertical" size={18} className="full-width">
                    <div className="analysis-record-header">
                      <div>
                        <Typography.Text type="secondary">当前会话</Typography.Text>
                        <Typography.Title level={5} className="analysis-record-title">
                          {formatRepoDisplay(recentAnalysis.repoUrl)}
                        </Typography.Title>
                      </div>
                      <Tag color={formatAnalysisStatus(recentAnalysis.status).color}>
                        {formatAnalysisStatus(recentAnalysis.status).label}
                      </Tag>
                    </div>

                    <Descriptions column={1} size="small" className="summary-descriptions">
                      <Descriptions.Item label="仓库">
                        {recentAnalysis.repoUrl}
                      </Descriptions.Item>
                      <Descriptions.Item label="PR 编号">
                        #{recentAnalysis.prNumber}
                      </Descriptions.Item>
                      <Descriptions.Item label="状态">
                        {formatAnalysisStatus(recentAnalysis.status).label}
                      </Descriptions.Item>
                      <Descriptions.Item label="耗时">
                        {formatDuration(recentAnalysis.durationMs)}
                      </Descriptions.Item>
                    </Descriptions>

                    {recentAnalysis.status === "FAILED" && recentAnalysis.errorMessage ? (
                      <Alert
                        type="error"
                        showIcon
                        message="本次分析未完成"
                        description={recentAnalysis.errorMessage}
                      />
                    ) : null}

                    {recentAnalysis.status === "SUCCESS" && latestCompletedReview ? (
                      <Button
                        type="primary"
                        ghost
                        icon={<CheckCircleOutlined />}
                        onClick={handleViewLatestReport}
                      >
                        查看报告
                      </Button>
                    ) : (
                      <Typography.Paragraph className="empty-state">
                        {recentAnalysis.status === "ANALYZING"
                          ? "报告生成完成后会自动进入结果页。"
                          : "提交新的仓库地址和 PR 编号后，这里会保留本次分析记录。"}
                      </Typography.Paragraph>
                    )}
                  </Space>
                ) : (
                  <Typography.Paragraph className="empty-state">
                    还没有提交分析任务。你可以先输入一个公开 GitHub 仓库地址，体验完整的 PR Review 流程。
                  </Typography.Paragraph>
                )}
              </Card>
            </Col>
          </Row>

          <Card className="status-card sample-report-card" bordered={false}>
            <Row gutter={[24, 24]} align="middle">
              <Col xs={24} lg={10}>
                <Space direction="vertical" size={12} className="full-width">
                  <Tag className="hero-tag" bordered={false}>
                    示例 Review 报告预览
                  </Tag>
                  <Typography.Title level={3} className="sample-report-title">
                    分析完成后，系统会输出结构化结果而不只是简单提示。
                  </Typography.Title>
                  <Typography.Paragraph className="hero-description">
                    你可以快速看到 PR 类型、总体风险、发现项数量、测试建议和可执行修复建议，
                    便于开发者与 Reviewer 直接进入后续确认与修复流程。
                  </Typography.Paragraph>
                </Space>
              </Col>
              <Col xs={24} lg={14}>
                <div className="sample-report-grid">
                  <div className="sample-report-metric">
                    <Typography.Text type="secondary">PR 类型</Typography.Text>
                    <Typography.Title level={4}>{sampleReportPreview.prType}</Typography.Title>
                  </div>
                  <div className="sample-report-metric">
                    <Typography.Text type="secondary">风险等级</Typography.Text>
                    <Typography.Title level={4}>{sampleReportPreview.riskLevel}</Typography.Title>
                  </div>
                  <div className="sample-report-metric">
                    <Typography.Text type="secondary">发现问题</Typography.Text>
                    <Typography.Title level={4}>{sampleReportPreview.findings} 个</Typography.Title>
                  </div>
                  <div className="sample-report-metric">
                    <Typography.Text type="secondary">测试建议</Typography.Text>
                    <Typography.Title level={4}>{sampleReportPreview.testSuggestions} 条</Typography.Title>
                  </div>
                  <div className="sample-report-metric">
                    <Typography.Text type="secondary">可执行修复建议</Typography.Text>
                    <Typography.Title level={4}>{sampleReportPreview.fixSuggestions} 条</Typography.Title>
                  </div>
                  <div className="sample-report-metric sample-report-metric-accent">
                    <Typography.Text type="secondary">报告特点</Typography.Text>
                    <Typography.Title level={4}>结构化、可复核、可反馈</Typography.Title>
                  </div>
                </div>
              </Col>
            </Row>
          </Card>
        </div>
      </div>
    </>
  );
}

export default HomePage;
