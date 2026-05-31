import { Alert, Button, Card, Form, Input, InputNumber, Space, Typography } from "antd";

const githubRepoUrlPattern =
  /^(https?:\/\/github\.com\/[^/\s]+\/[^/\s]+?(?:\.git)?\/?|git@github\.com:[^/\s]+\/[^/\s]+?(?:\.git)?)$/i;

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
            输入公开 GitHub 仓库地址和 PR 编号后，系统会自动识别 PR 类型、分析代码变更风险，
            并生成结构化 Review 报告、测试建议和修复示例。
          </Typography.Paragraph>
        </div>

        <Alert
          type="info"
          showIcon
          message="当前仅支持公开 GitHub 仓库"
          description="请使用有效的 GitHub 仓库地址，例如 https://github.com/owner/repository 或带 .git 的克隆地址。"
        />

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
              {
                validator: (_, value) => {
                  if (!value) {
                    return Promise.resolve();
                  }

                  if (!githubRepoUrlPattern.test(value.trim())) {
                    return Promise.reject(
                      new Error("仓库地址格式错误，请输入完整 GitHub URL")
                    );
                  }

                  return Promise.resolve();
                }
              }
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

          <Typography.Paragraph className="form-helper-text">
            预计需要 10-30 秒，分析完成后自动进入报告页。
          </Typography.Paragraph>
        </Form>
      </Space>
    </Card>
  );
}

export default PrInputForm;
