import axios from "axios";

const reviewApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "/api",
  timeout: 120000
});

function unwrapResponse(response) {
  const payload = response?.data;

  if (!payload || typeof payload !== "object") {
    throw new Error("后端返回了无法识别的响应格式。");
  }

  if (payload.code && payload.code !== 200) {
    throw new Error(payload.message || "Request failed.");
  }

  return payload.data;
}

async function resolveReviewPostNotFoundError() {
  try {
    await reviewApi.get("/reviews");
    return new Error("GitHub 仓库地址或 PR 编号未找到，请确认仓库公开可访问，并且该 PR 真实存在。");
  } catch {
    return new Error("后端 Review 接口不存在。请确认后端已重启，并且当前运行的是包含 /api/reviews 接口的最新版本。");
  }
}

async function buildRequestError(error, fallbackMessage) {
  const requestUrl = error?.config?.url ?? "";
  const statusCode = error?.response?.status;
  const responseMessage = error?.response?.data?.message;
  const statusText = error?.response?.statusText;

  if (statusCode === 404 && requestUrl === "/reviews" && error?.config?.method === "post") {
    return resolveReviewPostNotFoundError();
  }

  if (!error?.response) {
    return new Error("无法连接后端服务。请确认后端已启动，并且 http://localhost:8080 可访问。");
  }

  const message = responseMessage || statusText || error?.message || fallbackMessage;
  return new Error(message);
}

export async function createReviewTask(values) {
  try {
    const response = await reviewApi.post("/reviews", values);
    return unwrapResponse(response);
  } catch (error) {
    throw await buildRequestError(error, "创建 Review 任务失败，请稍后重试。");
  }
}

export async function getReviewResult(taskId) {
  try {
    const response = await reviewApi.get(`/reviews/${taskId}`);
    return unwrapResponse(response);
  } catch (error) {
    throw await buildRequestError(error, "获取 Review 结果失败，请稍后重试。");
  }
}

export async function submitFindingFeedback(taskId, findingId, payload) {
  try {
    const response = await reviewApi.post(
      `/reviews/${taskId}/findings/${findingId}/feedback`,
      payload
    );
    return unwrapResponse(response);
  } catch (error) {
    throw await buildRequestError(error, "提交反馈失败，请稍后重试。");
  }
}
