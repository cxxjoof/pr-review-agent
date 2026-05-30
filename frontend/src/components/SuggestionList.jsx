import { BulbOutlined, ExperimentOutlined } from "@ant-design/icons";
import { Card, Empty, List, Space, Typography } from "antd";

const iconMap = {
  review: <BulbOutlined />,
  test: <ExperimentOutlined />
};

function SuggestionList({ title, description, items = [], variant = "review" }) {
  return (
    <Card className="result-card" bordered={false}>
      <Space direction="vertical" size={18} className="full-width">
        <div className="card-header">
          <div>
            <Typography.Title level={4} className="card-title">
              {title}
            </Typography.Title>
            <Typography.Paragraph className="card-subtitle">
              {description}
            </Typography.Paragraph>
          </div>
        </div>

        {items.length > 0 ? (
          <List
            dataSource={items}
            split={false}
            renderItem={(item) => (
              <List.Item className="suggestion-item">
                <Space align="start" size={12}>
                  <div className="suggestion-icon">{iconMap[variant]}</div>
                  <Typography.Text>{item}</Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        ) : (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="当前结果没有生成对应建议。"
          />
        )}
      </Space>
    </Card>
  );
}

export default SuggestionList;
