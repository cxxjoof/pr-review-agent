import { Button, Card, Form, Input, InputNumber, Space, Typography } from "antd";

function PrInputForm({ loading, onSubmit }) {
  const [form] = Form.useForm();

  return (
    <Card className="form-card" bordered={false}>
      <Space direction="vertical" size={20} className="full-width">
        <div>
          <Typography.Title level={3} className="form-title">
            提交 PR 分析信息
          </Typography.Title>
          <Typography.Paragraph className="form-description">
            这里先提供基础录入能力，下一阶段再接入后端 Review 接口。
          </Typography.Paragraph>
        </div>

        <Form
          form={form}
          layout="vertical"
          size="large"
          initialValues={{
            repoUrl: "https://github.com/cxxjoof/pr-review-agent",
            prNumber: 1
          }}
          onFinish={onSubmit}
        >
          <Form.Item
            label="GitHub 仓库地址"
            name="repoUrl"
            rules={[
              { required: true, message: "请输入 GitHub 仓库地址" },
              { type: "url", message: "请输入有效的 URL" }
            ]}
          >
            <Input placeholder="https://github.com/owner/repository" />
          </Form.Item>

          <Form.Item
            label="PR 编号"
            name="prNumber"
            rules={[
              { required: true, message: "请输入 PR 编号" },
              {
                validator: (_, value) => {
                  if (value === undefined || value === null || value === "") {
                    return Promise.reject(new Error("请输入 PR 编号"));
                  }

                  if (!Number.isInteger(value) || value <= 0) {
                    return Promise.reject(new Error("PR 编号必须是大于 0 的整数"));
                  }

                  return Promise.resolve();
                }
              }
            ]}
          >
            <InputNumber
              min={1}
              precision={0}
              controls={false}
              placeholder="例如：1"
              className="full-width"
            />
          </Form.Item>

          <Form.Item className="form-actions">
            <Button type="primary" htmlType="submit" loading={loading} block>
              开始分析
            </Button>
          </Form.Item>
        </Form>
      </Space>
    </Card>
  );
}

export default PrInputForm;
