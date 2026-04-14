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
        
        <div class="grouped-list">
          <div v-if="groupedMessages.length === 0" class="empty-state">暂无符合条件的消息</div>
          
          <div 
            v-for="group in groupedMessages" 
            :key="group.companyId" 
            class="company-group"
          >
            <div 
              class="group-header" 
              @click="toggleGroup(group.companyId)"
            >
              <div class="group-info">
                <span class="company-id">{{ group.companyId }}</span>
                <span class="alert-count">{{ group.alerts.length }}条预警</span>
                <span v-if="group.hasHighRisk" class="high-risk-badge">含高风险</span>
              </div>
              <div class="group-actions">
                <span class="expand-icon">{{ expandedGroups.includes(group.companyId) ? '▼' : '▶' }}</span>
              </div>
            </div>
            
            <div 
              v-show="expandedGroups.includes(group.companyId)" 
              class="group-content"
            >
              <table class="alert-table">
                <thead>
                  <tr>
                    <th>预警ID</th>
                    <th>等级</th>
                    <th>原因</th>
                    <th>状态</th>
                    <th>动作结果</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="alert in group.alerts" :key="alert.alertId">
                    <td>{{ alert.alertId }}</td>
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
const expandedGroups = ref([]);

const filters = ref({
  level: '',
  status: '',
  search: ''
});

const stats = computed(() => {
  const critical = messages.value.filter(m => m.alertLevel === 'CRITICAL').length;
  const high = messages.value.filter(m => m.alertLevel === 'HIGH').length;
  const medium = messages.value.filter(m => m.alertLevel === 'MEDIUM').length;
  const open = messages.value.filter(m => m.status === 'open').length;
  return { critical, high, medium, open };
});

const unreadCount = computed(() => {
  return messages.value.filter(m => m.status === 'open').length;
});

const filteredMessages = computed(() => {
  let result = [...messages.value];
  
  if (filters.value.level) {
    result = result.filter(m => m.alertLevel === filters.value.level);
  }
  
  if (filters.value.status) {
    result = result.filter(m => m.status === filters.value.status);
  }
  
  if (filters.value.search) {
    const search = filters.value.search.toLowerCase();
    result = result.filter(m => 
      m.companyId.toLowerCase().includes(search) ||
      (m.reason && m.reason.toLowerCase().includes(search))
    );
  }
  
  return result;
});

const groupedMessages = computed(() => {
  const groups = {};
  
  filteredMessages.value.forEach(msg => {
    if (!groups[msg.companyId]) {
      groups[msg.companyId] = {
        companyId: msg.companyId,
        alerts: [],
        hasHighRisk: false
      };
    }
    groups[msg.companyId].alerts.push(msg);
    if (msg.alertLevel === 'CRITICAL' || msg.alertLevel === 'HIGH') {
      groups[msg.companyId].hasHighRisk = true;
    }
  });
  
  return Object.values(groups).sort((a, b) => {
    if (a.hasHighRisk !== b.hasHighRisk) {
      return a.hasHighRisk ? -1 : 1;
    }
    return b.alerts.length - a.alerts.length;
  }).map(group => ({
    ...group,
    alerts: group.alerts.sort((x, y) => {
      const levelOrder = { CRITICAL: 0, HIGH: 1, MEDIUM: 2 };
      if (levelOrder[x.alertLevel] !== levelOrder[y.alertLevel]) {
        return levelOrder[x.alertLevel] - levelOrder[y.alertLevel];
      }
      return new Date(y.createdAt) - new Date(x.createdAt);
    })
  }));
});

function toggleGroup(companyId) {
  const index = expandedGroups.value.indexOf(companyId);
  if (index > -1) {
    expandedGroups.value.splice(index, 1);
  } else {
    expandedGroups.value.push(companyId);
  }
}

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
    const groupedData = res.data || [];
    messages.value = groupedData.flatMap(batch => batch.alerts || []);
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

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th {
  text-align: left;
  padding: 12px 16px;
  background: #f8fafc;
  font-weight: 600;
  color: #334155;
  border-bottom: 2px solid #e2e8f0;
}

td {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  color: #475569;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 48px 24px;
  font-style: italic;
}

.grouped-list {
  margin-top: 8px;
}

.company-group {
  margin-bottom: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: #f8fafc;
  cursor: pointer;
  transition: background 0.2s ease;
}

.group-header:hover {
  background: #f1f5f9;
}

.group-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.company-id {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

.alert-count {
  font-size: 13px;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.high-risk-badge {
  font-size: 12px;
  color: #dc2626;
  background: #fef2f2;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.group-actions {
  display: flex;
  align-items: center;
}

.expand-icon {
  font-size: 12px;
  color: #64748b;
  transition: transform 0.2s ease;
}

.group-content {
  padding: 0;
  background: white;
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