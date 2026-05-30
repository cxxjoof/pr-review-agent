import { Card, Empty, Space, Table, Tag, Typography } from "antd";
import { formatRiskType, getRiskLevelMeta } from "../utils/riskLevel.js";

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

function RiskItemTable({ riskItems = [] }) {
  const columns = [
    {
      title: "风险等级",
      dataIndex: "riskLevel",
      key: "riskLevel",
      width: 120,
      render: (riskLevel) => {
        const meta = getRiskLevelMeta(riskLevel);
        return <Tag color={meta.color}>{meta.label}</Tag>;
      }
    },
    {
      title: "位置",
      key: "location",
      width: 260,
      render: (_, item) => (
        <Space direction="vertical" size={2}>
          <Typography.Text strong>{item.filePath || "未知文件"}</Typography.Text>
          <Typography.Text type="secondary">
            {item.lineNumber ? `行号：${item.lineNumber}` : "行号：未提供"}
          </Typography.Text>
        </Space>
      )
    },
    {
      title: "风险描述",
      key: "description",
      render: (_, item) => (
        <Space direction="vertical" size={8}>
          <Typography.Text>{item.description || "模型未给出风险描述。"}</Typography.Text>
          {item.codeSnippet ? (
            <Typography.Paragraph className="code-snippet">
              {item.codeSnippet}
            </Typography.Paragraph>
          ) : null}
        </Space>
      )
    },
    {
      title: "风险类型",
      dataIndex: "riskType",
      key: "riskType",
      width: 150,
      render: (riskType) => formatRiskType(riskType)
    },
    {
      title: "建议",
      dataIndex: "suggestion",
      key: "suggestion",
      width: 220,
      render: (suggestion) => suggestion || "建议人工复核这一段改动。"
    },
    {
      title: "置信度",
      dataIndex: "confidence",
      key: "confidence",
      width: 100,
      render: (confidence) => renderConfidence(confidence)
    }
  ];

  return (
    <Card className="result-card" bordered={false}>
      <Space direction="vertical" size={18} className="full-width">
        <div className="card-header">
          <div>
            <Typography.Title level={4} className="card-title">
              风险代码列表
            </Typography.Title>
            <Typography.Paragraph className="card-subtitle">
              聚焦模型标出的潜在问题位置，方便你快速回到 diff 进行人工复查。
            </Typography.Paragraph>
          </div>
        </div>

        {riskItems.length > 0 ? (
          <Table
            rowKey={(item, index) =>
              `${item.filePath ?? "unknown"}-${item.lineNumber ?? "line"}-${index}`
            }
            columns={columns}
            dataSource={riskItems}
            pagination={false}
            scroll={{ x: 1080 }}
          />
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="当前结果没有识别到明确风险项，建议仍然结合实际业务做一次人工确认。"
          />
        )}
      </Space>
    </Card>
  );
}

export default RiskItemTable;
