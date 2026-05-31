export const findingLevelMetaMap = {
  HIGH: {
    label: "高风险",
    color: "error",
    order: 4
  },
  MEDIUM: {
    label: "中风险",
    color: "warning",
    order: 3
  },
  LOW: {
    label: "低风险",
    color: "success",
    order: 2
  },
  ADVISORY: {
    label: "建议项",
    color: "blue",
    order: 1
  }
};

const findingCategoryLabelMap = {
  DOCUMENTATION_FORMAT: "文档格式问题",
  COMMAND_EXECUTABILITY: "命令可执行性问题",
  PATH_COMPATIBILITY: "路径兼容性问题",
  TEST_GAP: "测试缺口",
  CODE_LOGIC: "代码逻辑风险",
  SECURITY: "安全风险",
  EXCEPTION_HANDLING: "异常处理问题",
  PERFORMANCE: "性能问题",
  MAINTAINABILITY: "可维护性问题",
  CONFIG_COMPATIBILITY: "配置兼容性问题",
  DEPENDENCY_CHANGE: "依赖变更问题",
  CICD_CHANGE: "CI/CD 变更问题",
  OTHER: "其他发现项"
};

const feedbackLabelMap = {
  USEFUL: "有用",
  FALSE_POSITIVE: "误报",
  IGNORED: "已忽略",
  FIXED: "已修复"
};

export function getFindingLevelMeta(level) {
  if (!level) {
    return {
      label: "未标注",
      color: "default",
      order: 0
    };
  }

  return (
    findingLevelMetaMap[level] ?? {
      label: level,
      color: "default",
      order: 0
    }
  );
}

export function formatFindingCategory(category) {
  if (!category) {
    return "未分类";
  }

  return findingCategoryLabelMap[category] ?? category;
}

export function formatFeedbackStatus(status) {
  if (!status) {
    return "未反馈";
  }

  return feedbackLabelMap[status] ?? status;
}

export function getResultSectionTitle(prType) {
  if (prType === "DOCUMENTATION") {
    return "文档问题列表";
  }

  if (prType === "CODE" || prType === "MIXED") {
    return "风险代码列表";
  }

  return "Review 发现项";
}
