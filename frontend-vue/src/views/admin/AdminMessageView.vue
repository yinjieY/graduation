<template>
  <Layout role="admin" :unreadCount="unreadCount">
    <div class="message-management">
      <div class="page-header">
        <h1 class="page-title">系统消息</h1>
        <p class="page-subtitle">查看系统预警和通知</p>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon critical">
            <span class="icon">!</span>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.critical }}</div>
            <div class="stat-label">严重预警</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon high">
            <span class="icon">!</span>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.high }}</div>
            <div class="stat-label">高风险预警</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon medium">
            <span class="icon">!</span>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.medium }}</div>
            <div class="stat-label">中等预警</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon open">
            <span class="icon">○</span>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stats.open }}</div>
            <div class="stat-label">待处理</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>系统消息列表</h3>
          <div class="header-actions">
            <div class="filter-group">
              <select v-model="filters.level" class="filter-select">
                <option value="">全部等级</option>
                <option value="CRITICAL">严重</option>
                <option value="HIGH">高风险</option>
                <option value="MEDIUM">中等</option>
              </select>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部状态</option>
                <option value="open">待处理</option>
                <option value="closed">已处理</option>
              </select>
              <input 
                v-model="filters.search" 
                type="text" 
                class="filter-input" 
                placeholder="搜索企业ID或原因..."
                @keyup.enter="applyFilters"
              />
            </div>
            <button class="btn-refresh" :disabled="loading" @click="loadMessages">
              <span v-if="loading" class="loading-spinner"></span>
              刷新消息
            </button>
          </div>
        </div>
        
        <div class="batch-groups">
          <div v-if="filteredMessages.length === 0" class="empty-state">暂无符合条件的消息</div>
          
          <div v-for="group in filteredMessages" :key="group.batchId || group.batchName" class="batch-group">
            <div class="batch-header">
              <div class="batch-info">
                <span class="batch-icon">📦</span>
                <span class="batch-name">{{ group.batchName || group.batchId || '未命名批次' }}</span>
              </div>
              <div class="batch-stats">
                <span class="stat-item">{{ group.alerts?.length || 0 }} 条预警</span>
                <span class="stat-item critical-count" v-if="group.criticalRiskCount > 0">高风险 {{ group.criticalRiskCount }}</span>
                <span class="stat-item high-count" v-if="group.highRiskCount > 0">高风险 {{ group.highRiskCount }}</span>
                <span class="stat-item medium-count" v-if="group.mediumRiskCount > 0">中风险 {{ group.mediumRiskCount }}</span>
              </div>
            </div>
            
            <div class="qs-id-groups">
              <div v-for="qsGroup in groupAlertsByQsId(group.alerts)" :key="qsGroup.qsId" class="qs-id-group">
                <div class="qs-id-header" @click="toggleQsId(qsGroup.qsId)">
                  <span class="collapse-icon" :class="{ expanded: !collapsedQsIds.has(qsGroup.qsId) }">▶</span>
                  <span class="qs-id-icon">📱</span>
                  <span class="qs-id-name">{{ qsGroup.qsId }}</span>
                  <span class="qs-id-stats">
                    {{ qsGroup.alerts.length }} 条预警
                    <span v-if="qsGroup.riskSummary.HIGH > 0 || qsGroup.riskSummary.CRITICAL > 0" class="qs-high-count">高风险 {{ qsGroup.riskSummary.HIGH + qsGroup.riskSummary.CRITICAL }}</span>
                    <span v-if="qsGroup.riskSummary.MEDIUM > 0" class="qs-medium-count">中风险 {{ qsGroup.riskSummary.MEDIUM }}</span>
                  </span>
                </div>
                
                <div v-show="!collapsedQsIds.has(qsGroup.qsId)" class="alert-list">
                  <table class="alert-table">
                    <thead>
                      <tr>
                        <th>预警ID</th>
                        <th>企业</th>
                        <th>等级</th>
                        <th>原因</th>
                        <th>状态</th>
                        <th>动作结果</th>
                        <th>时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="alert in qsGroup.alerts" :key="alert.alertId">
                        <td>{{ alert.alertId }}</td>
                        <td>{{ alert.companyId }}</td>
                        <td>
                          <span :class="getLevelClass(alert.alertLevel)">
                            {{ getLevelText(alert.alertLevel) }}
                          </span>
                        </td>
                        <td class="reason-cell">{{ alert.reason }}</td>
                        <td>
                          <span :class="{ 'status-open': alert.status === 'open', 'status-closed': alert.status === 'closed' }">
                            {{ alert.status === 'open' ? '待处理' : '已处理' }}
                          </span>
                        </td>
                        <td>{{ alert.actionResult || '-' }}</td>
                        <td>{{ formatTime(alert.createdAt) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import { getMessages } from '../../api/alert';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const messages = ref([]);
const token = ref(getToken('admin'));
const collapsedQsIds = ref(new Set());

const filters = ref({
  level: '',
  status: '',
  search: ''
});

// 按二维码ID分组预警消息
function groupAlertsByQsId(alerts) {
  const groups = {};
  (alerts || []).forEach(alert => {
    const qsId = alert.qsId || 'unknown';
    if (!groups[qsId]) {
      groups[qsId] = {
        qsId,
        alerts: [],
        riskSummary: {
          HIGH: 0,
          CRITICAL: 0,
          MEDIUM: 0,
          LOW: 0
        }
      };
    }
    groups[qsId].alerts.push(alert);
    const level = alert.alertLevel || 'LOW';
    if (groups[qsId].riskSummary[level] !== undefined) {
      groups[qsId].riskSummary[level]++;
    }
  });
  return Object.values(groups);
}

// 切换二维码折叠状态
function toggleQsId(qsId) {
  if (collapsedQsIds.value.has(qsId)) {
    collapsedQsIds.value.delete(qsId);
  } else {
    collapsedQsIds.value.add(qsId);
  }
}

// 获取扁平化的预警列表用于统计
const flatAlerts = computed(() => {
  return messages.value.flatMap(batch => batch.alerts || []);
});

const stats = computed(() => {
  const critical = flatAlerts.value.filter(m => m.alertLevel === 'CRITICAL').length;
  const high = flatAlerts.value.filter(m => m.alertLevel === 'HIGH').length;
  const medium = flatAlerts.value.filter(m => m.alertLevel === 'MEDIUM').length;
  const open = flatAlerts.value.filter(m => m.status === 'open').length;
  return { critical, high, medium, open };
});

const unreadCount = computed(() => {
  return flatAlerts.value.filter(m => m.status === 'open').length;
});

const filteredMessages = computed(() => {
  return messages.value.map(batch => ({
    ...batch,
    alerts: (batch.alerts || []).filter(alert => {
      if (filters.value.level && alert.alertLevel !== filters.value.level) {
        return false;
      }
      if (filters.value.status && alert.status !== filters.value.status) {
        return false;
      }
      if (filters.value.search) {
        const search = filters.value.search.toLowerCase();
        const matchCompany = alert.companyId.toLowerCase().includes(search);
        const matchReason = alert.reason && alert.reason.toLowerCase().includes(search);
        if (!matchCompany && !matchReason) {
          return false;
        }
      }
      return true;
    })
  })).filter(batch => batch.alerts.length > 0);
});

function applyFilters() {}

function getLevelClass(level) {
  switch(level) {
    case 'CRITICAL': return 'level-critical';
    case 'HIGH': return 'level-high';
    case 'MEDIUM': return 'level-medium';
    default: return 'level-low';
  }
}

function getLevelText(level) {
  switch(level) {
    case 'CRITICAL': return '严重';
    case 'HIGH': return '高风险';
    case 'MEDIUM': return '中等';
    default: return level;
  }
}

function formatTime(time) {
  if (!time) return '-';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

async function loadMessages() {
  loading.value = true;
  try {
    const res = await getMessages(token.value);
    messages.value = res.data || [];  // 保持后端分组结构
  } catch (error) {
    console.error('加载系统消息失败:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadMessages();
});
</script>

<style scoped>
.message-management {
  width: 100%;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.critical {
  background: #fef2f2;
}

.stat-icon.critical .icon {
  color: #dc2626;
  font-size: 20px;
  font-weight: 700;
}

.stat-icon.high {
  background: #fff7ed;
}

.stat-icon.high .icon {
  color: #ea580c;
  font-size: 20px;
  font-weight: 700;
}

.stat-icon.medium {
  background: #eff6ff;
}

.stat-icon.medium .icon {
  color: #2563eb;
  font-size: 20px;
  font-weight: 700;
}

.stat-icon.open {
  background: #f0fdf4;
}

.stat-icon.open .icon {
  color: #16a34a;
  font-size: 20px;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
}

.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #475569;
  background: white;
  cursor: pointer;
  min-width: 120px;
}

.filter-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.filter-input {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #475569;
  min-width: 200px;
}

.filter-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 48px 24px;
  font-style: italic;
}

/* 批次分组样式 */
.batch-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.batch-group {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-icon {
  font-size: 18px;
}

.batch-name {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.batch-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.stat-item {
  color: #64748b;
}

.stat-item.critical-count,
.stat-item.high-count {
  color: #ef4444;
  font-weight: 500;
}

.stat-item.medium-count {
  color: #f59e0b;
  font-weight: 500;
}

/* 二维码分组样式 */
.qs-id-groups {
  display: flex;
  flex-direction: column;
}

.qs-id-group {
  border-bottom: 1px solid #e2e8f0;
}

.qs-id-group:last-child {
  border-bottom: none;
}

.qs-id-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #fafafa;
  cursor: pointer;
  transition: background 0.2s ease;
}

.qs-id-header:hover {
  background: #f1f5f9;
}

.collapse-icon {
  font-size: 10px;
  color: #94a3b8;
  transition: transform 0.2s ease;
  width: 14px;
  text-align: center;
}

.collapse-icon.expanded {
  transform: rotate(90deg);
}

.qs-id-icon {
  font-size: 16px;
}

.qs-id-name {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  font-family: 'Courier New', monospace;
}

.qs-id-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
  margin-left: auto;
}

.qs-high-count {
  color: #ef4444;
  font-weight: 500;
}

.qs-medium-count {
  color: #f59e0b;
  font-weight: 500;
}

.alert-list {
  overflow-x: auto;
}

.alert-list table {
  margin-bottom: 0;
}

.alert-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.alert-table th {
  background: #fff;
  padding: 10px 16px;
  font-weight: 600;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e2e8f0;
}

.alert-table td {
  padding: 10px 16px;
  border-bottom: 1px solid #f1f5f9;
  color: #475569;
}

.alert-table tbody tr:hover {
  background: #fafafa;
}

.reason-cell {
  max-width: 300px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.level-critical {
  background: #fef2f2;
  color: #dc2626;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.level-high {
  background: #fff7ed;
  color: #ea580c;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.level-medium {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.level-low {
  background: #f1f5f9;
  color: #64748b;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.status-open {
  background: #fef2f2;
  color: #dc2626;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-closed {
  background: #f0fdf4;
  color: #16a34a;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .batch-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .batch-stats {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .qs-id-header {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .qs-id-stats {
    width: 100%;
    justify-content: flex-start;
    margin-left: 0;
  }
  
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }
  
  .filter-group {
    flex-wrap: wrap;
  }
  
  .filter-input {
    min-width: 100%;
  }
}
</style>
