export function mapAlertItem(item) {
  if (!item) return {};
  return {
    alertId: item.alertId || item.alert_id,
    qsId: item.qsId || item.qs_id || '-',
    companyId: item.companyId || item.company_id,
    alertLevel: item.alertLevel || item.alert_level || 'LOW',
    reason: item.reason || '-',
    detail: item.detail || '-',
    status: item.status || 'OPEN',
    actionResult: item.actionResult || item.action_result || '-',
    pushStatus: item.pushStatus || item.push_status || '-',
    createdAt: item.createdAt || item.created_at || '-',
    batchId: item.batchId || item.batch_id || '-',
    batchName: item.batchName || item.batch_name || '未知批次',
    isRead: item.isRead || false
  };
}

export function mapBatchItem(item) {
  if (!item) return {};
  return {
    batchId: item.batchId || item.batch_id,
    batchName: item.batchName || item.batch_name || item.productionStandard || '-',
    productionDate: item.productionDate || item.production_date || '-',
    quantity: item.quantity || item.totalQuantity || item.total_quantity || 0,
    description: item.description || item.ingredients || '-',
    status: item.status || item.reviewStatus || 'DRAFT',
    reviewComment: item.reviewComment || item.review_comment || '-',
    hasQrCodes: item.hasQrCodes || false
  };
}

export function mapQrCodeItem(item) {
  if (!item) return {};
  return {
    qsId: item.qsId || item.qs_id,
    batchId: item.batchId || item.batch_id || '-',
    batchName: item.batchName || item.batch_name || '-',
    companyId: item.companyId || item.company_id,
    companyName: item.companyName || '-',
    status: item.status || 'active',
    createdAt: item.createdAt || item.created_at || '-',
    updatedAt: item.updatedAt || item.updated_at || '-',
    maxAllowedScans: item.maxAllowedScans || item.max_allowed_scans || 5
  };
}

export function mapCompanyItem(item) {
  if (!item) return {};
  return {
    companyId: item.companyId || item.company_id,
    name: item.name || item.companyName || '-',
    phone: item.phone || '-',
    address: item.address || '-',
    status: item.status || 'DRAFT',
    createdAt: item.createdAt || item.created_at || '-',
    updatedAt: item.updatedAt || item.updated_at || '-'
  };
}