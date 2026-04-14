export const AlertStatus = {
  OPEN: 'OPEN',
  PROCESSED: 'PROCESSED',
  CLOSED: 'CLOSED'
};

export const RiskLevel = {
  HIGH: 'HIGH',
  MEDIUM: 'MEDIUM',
  LOW: 'LOW'
};

export const QsCodeStatus = {
  ACTIVE: 'ACTIVE',
  FROZEN: 'FROZEN',
  USED: 'USED',
  EXPIRED: 'EXPIRED'
};

export const BatchStatus = {
  DRAFT: 'DRAFT',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
};

export const CompanyStatus = {
  DRAFT: 'DRAFT',
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED'
};

export const UserRole = {
  ADMIN: 'ADMIN',
  COMPANY: 'COMPANY'
};

export const MessageType = {
  ALERT: 'ALERT',
  SYSTEM: 'SYSTEM',
  NOTICE: 'NOTICE'
};

export const FeedbackStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  RESOLVED: 'RESOLVED'
};

export const ProofStatus = {
  PENDING: 'PENDING',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED'
};

export function getRiskLevelLabel(level) {
  const labels = {
    HIGH: '高风险',
    MEDIUM: '中风险',
    LOW: '低风险'
  };
  return labels[level] || level;
}

export function getStatusLabel(status, type) {
  const statusMaps = {
    alert: {
      OPEN: '待处理',
      PROCESSED: '处理中',
      CLOSED: '已关闭'
    },
    batch: {
      DRAFT: '草稿',
      PENDING: '待审核',
      APPROVED: '已通过',
      REJECTED: '已拒绝'
    },
    company: {
      DRAFT: '草稿',
      PENDING: '待审核',
      APPROVED: '已通过',
      REJECTED: '已拒绝'
    },
    qrcode: {
      ACTIVE: '正常',
      FROZEN: '冻结',
      USED: '已使用',
      EXPIRED: '已过期'
    },
    feedback: {
      PENDING: '待处理',
      PROCESSING: '处理中',
      RESOLVED: '已解决'
    },
    proof: {
      PENDING: '待上链',
      SUCCESS: '上链成功',
      FAILED: '上链失败'
    }
  };
  const map = statusMaps[type] || {};
  return map[status] || status;
}