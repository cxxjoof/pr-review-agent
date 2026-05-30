export const riskLevelMetaMap = {
  HIGH: {
    label: "高风险",
    color: "error"
  },
  MEDIUM: {
    label: "中风险",
    color: "warning"
  },
  LOW: {
    label: "低风险",
    color: "success"
  }
};

const riskTypeLabelMap = {
  NULL_POINTER: "空指针风险",
  EXCEPTION_HANDLING: "异常处理",
  SECURITY: "安全问题",
  PERFORMANCE: "性能问题",
  INPUT_VALIDATION: "输入校验",
  DATABASE: "数据库影响",
  CONFIG_CHANGE: "配置变更",
  TEST_MISSING: "测试缺失",
  CODE_STYLE: "代码风格",
  OTHER: "其他风险"
};

export function getRiskLevelMeta(level) {
  if (!level) {
    return {
      label: "未标注",
      color: "default"
    };
  }

  return (
    riskLevelMetaMap[level] ?? {
      label: level,
      color: "default"
    }
  );
}

export function formatRiskType(riskType) {
  if (!riskType) {
    return "未分类";
  }

  return riskTypeLabelMap[riskType] ?? riskType;
}
