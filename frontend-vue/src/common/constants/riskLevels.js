export const RISK_LEVELS = {
  CRITICAL: 'CRITICAL',
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW'
};

export const RISK_LEVEL_MAP = {
  CRITICAL: { label: '极高风险', color: '#dc2626', class: 'level-critical' },
  HIGH: { label: '高风险', color: '#ea580c', class: 'level-high' },
  MEDIUM: { label: '中风险', color: '#ca8a04', class: 'level-medium' },
  LOW: { label: '低风险', color: '#059669', class: 'level-low' }
};

export function getRiskLevel(score) {
  const scorePercent = score * 100;
  if (scorePercent >= 86) {
    return RISK_LEVELS.CRITICAL;
  } else if (scorePercent >= 61) {
    return RISK_LEVELS.HIGH;
  } else if (scorePercent >= 31) {
    return RISK_LEVELS.MEDIUM;
  } else {
    return RISK_LEVELS.LOW;
  }
}

export const ROLES = {
  ADMIN: 'ADMIN',
  COMPANY: 'COMPANY',
  CONSUMER: 'CONSUMER'
};

export const ROLE_MAP = {
  ADMIN: '管理员',
  COMPANY: '企业用户',
  CONSUMER: '消费者'
};