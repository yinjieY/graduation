<template>
  <Layout :role="'company'">
    <div class="alert-center">
      <h1 class="page-title">预警中心</h1>
      
      <div class="alert-filter">
        <div class="filter-left">
          <input 
            v-model="searchKeyword" 
            type="text" 
            placeholder="搜索批次ID或批次名称" 
            class="search-input"
            @input="handleSearch"
          />
          <select v-model="filterLevel" class="filter-select">
            <option value="">全部级别</option>
            <option value="HIGH">高风险</option>
            <option value="MEDIUM">中风险</option>
            <option value="LOW">低风险</option>
          </select>
          <select v-model="filterStatus" class="filter-select">
            <option value="">全部状态</option>
            <option value="OPEN">未闭环</option>
            <option value="CLOSED">已闭环</option>
          </select>
        </div>
        <div class="filter-right">
          <button 
            class="btn-mark-all-read" 
            :disabled="loading || unreadCount === 0" 
            @click="handleMarkAllAsRead"
          >
            <span v-if="loading" class="loading-spinner"></span>
            <svg v-else class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 6.253v11.494m-6.75-8.494h13.5M4 12h16M9 4l3 3 3-3m-3 16v-4"/>
            </svg>
            <span class="btn-text">全部已读</span>
            <span v-if="unreadCount > 0" class="unread-badge-btn">{{ unreadCount }}</span>
          </button>
          <button 
            class="btn-refresh" 
            :disabled="loading" 
            @click="loadAlerts"
          >
            <span v-if="loading" class="loading-spinner"></span>
            <svg v-else class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
            <span class="btn-text">刷新</span>
          </button>
        </div>
      </div>
      
      <div class="batch-groups">
        <div v-if="filteredGroups.length === 0" class="empty-state">
          <p>暂无预警数据</p>
        </div>
        
        <div v-for="group in filteredGroups" :key="group.batchId" class="batch-group">
          <div class="batch-header" @click="toggleCollapse(group.batchId)">
            <div class="batch-info">
              <span class="collapse-icon" :class="{ expanded: expandedGroups.includes(group.batchId) }">
                ▼
              </span>
              <span class="batch-icon">📦</span>
              <span class="batch-name">{{ group.batchId || '未知批次' }}</span>
              <span class="batch-name-label" v-if="group.batchName">{{ group.batchName }}</span>
            </div>
            <div class="batch-stats">
              <span class="stat-item">{{ group.alerts.length }} 条预警</span>
              <span class="stat-item high-count" v-if="group.highCount > 0">高风险 {{ group.highCount }}</span>
              <span class="stat-item medium-count" v-if="group.mediumCount > 0">中风险 {{ group.mediumCount }}</span>
              <span class="stat-item low-count" v-if="group.lowCount > 0">低风险 {{ group.lowCount }}</span>
            </div>
          </div>
          
          <div v-show="expandedGroups.includes(group.batchId)" class="alert-list">
            <div class="alert-item" v-for="alert in group.alerts" :key="alert.id" :class="`alert-${alert.level}`">
              <div class="alert-header">
                <div class="alert-level" :class="`level-${alert.level}`">
                  {{ alert.level === 'HIGH' ? '高风险' : alert.level === 'MEDIUM' ? '中风险' : '低风险' }}
                </div>
                <div class="alert-status" :class="`status-${alert.status}`">
                  {{ alert.status === 'OPEN' ? '未闭环' : '已闭环' }}
                </div>
                <div v-if="!alert.isRead" class="unread-badge">未读</div>
              </div>
              <div class="alert-content">
                <h3 class="alert-title">{{ alert.title }}</h3>
                <p class="alert-description">{{ alert.description }}</p>
                <div class="alert-meta">
                  <span class="meta-item">时间: {{ alert.createdAt }}</span>
                  <span class="meta-item">关联码: {{ alert.qsId }}</span>
                </div>
              </div>
              <div class="alert-actions">
                <BaseButton type="primary" size="small" @click="handleViewDetail(alert)">查看详情</BaseButton>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="showDetailDialog" class="dialog-overlay" @click="showDetailDialog = false">
        <div class="dialog-content" @click.stop>
          <h2>预警详情</h2>
          <div class="alert-detail">
            <div class="detail-item">
              <label>预警级别:</label>
              <span :class="`level-${currentAlert.level}`">
                {{ currentAlert.level === 'HIGH' ? '高风险' : currentAlert.level === 'MEDIUM' ? '中风险' : '低风险' }}
              </span>
            </div>
            <div class="detail-item">
              <label>预警状态:</label>
              <span :class="`status-${currentAlert.status}`">
                {{ currentAlert.status === 'OPEN' ? '未闭环' : '已闭环' }}
              </span>
            </div>
            <div class="detail-item">
              <label>所属批次:</label>
              <span>{{ currentAlert.batchName || currentAlert.batchId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>预警标题:</label>
              <span>{{ currentAlert.title }}</span>
            </div>
            <div class="detail-item">
              <label>预警描述:</label>
              <span>{{ currentAlert.description }}</span>
            </div>
            <div class="detail-item">
              <label>关联码:</label>
              <span>{{ currentAlert.qsId }}</span>
            </div>
            <div class="detail-item">
              <label>创建时间:</label>
              <span>{{ currentAlert.createdAt }}</span>
            </div>
            <div class="detail-item" v-if="currentAlert.processedAt">
              <label>处理时间:</label>
              <span>{{ currentAlert.processedAt }}</span>
            </div>
            <div class="detail-item" v-if="currentAlert.processNote">
              <label>处理备注:</label>
              <span>{{ currentAlert.processNote }}</span>
            </div>
          </div>
          <div class="form-actions">
            <BaseButton type="secondary" @click="showDetailDialog = false">关闭</BaseButton>
          </div>
        </div>
      </div>
      
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';

const api = useApi();
const { showError, showSuccess } = useNotification();

const loading = ref(false);
const showDetailDialog = ref(false);
const filterLevel = ref('');
const filterStatus = ref('');
const searchKeyword = ref('');
const currentAlert = ref({});
const alerts = ref([]);
const expandedGroups = ref([]);
const unreadCount = computed(() => {
  return alerts.value.reduce((total, group) => {
    return total + (group.alerts || []).filter(alert => !alert.isRead).length;
  }, 0);
});

function formatDateTime(dateStr) {
  if (!dateStr) return '-';
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '-';
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  } catch {
    return '-';
  }
}

function mapAlertItem(item) {
  return {
    id: item.alertId,
    level: String(item.alertLevel || 'LOW').toUpperCase(),
    status: String(item.status || 'OPEN').toUpperCase(),
    title: item.reason || '风险预警',
    description: item.detail || '-',
    qsId: item.qsId || '-',
    createdAt: formatDateTime(item.createdAt),
    processedAt: item.status === 'CLOSED' ? formatDateTime(item.createdAt) : '',
    processNote: item.actionResult || '',
    batchId: item.batchId || '',
    batchName: item.batchName || '',
    isRead: item.isRead || false
  };
}

const groupedAlerts = computed(() => {
  return alerts.value
    .map(group => {
      const mappedAlerts = (group.alerts || []).map(mapAlertItem);
      const filteredAlerts = mappedAlerts
        .filter(alert => !filterLevel.value || alert.level === filterLevel.value)
        .filter(alert => !filterStatus.value || alert.status === filterStatus.value);
      
      const highCount = filteredAlerts.filter(a => a.level === 'HIGH').length;
      const mediumCount = filteredAlerts.filter(a => a.level === 'MEDIUM').length;
      const lowCount = filteredAlerts.filter(a => a.level === 'LOW').length;
      
      return {
        batchId: group.batchId || 'unknown',
        batchName: group.batchName || '未知批次',
        alerts: filteredAlerts,
        highCount,
        mediumCount,
        lowCount
      };
    })
    .filter(group => group.alerts.length > 0)
    .sort((a, b) => {
      if (b.highCount !== a.highCount) {
        return b.highCount - a.highCount;
      }
      const aFirst = a.alerts[0]?.createdAt || '';
      const bFirst = b.alerts[0]?.createdAt || '';
      return new Date(bFirst) - new Date(aFirst);
    });
});

const filteredGroups = computed(() => {
  if (!searchKeyword.value.trim()) {
    return groupedAlerts.value;
  }
  const keyword = searchKeyword.value.toLowerCase().trim();
  return groupedAlerts.value.filter(group => {
    const batchIdMatch = (group.batchId || '').toLowerCase().includes(keyword);
    const batchNameMatch = (group.batchName || '').toLowerCase().includes(keyword);
    return batchIdMatch || batchNameMatch;
  });
});

const toggleCollapse = (batchId) => {
  const index = expandedGroups.value.indexOf(batchId);
  if (index > -1) {
    expandedGroups.value.splice(index, 1);
  } else {
    expandedGroups.value.push(batchId);
  }
};

const markAsRead = async (alertId) => {
  try {
    const token = localStorage.getItem('company_token');
    await api.markAlertAsRead(alertId, token);
    alerts.value = alerts.value.map(group => ({
      ...group,
      alerts: (group.alerts || []).map(alert => 
        alert.id === alertId ? { ...alert, isRead: true } : alert
      )
    }));
    updateUnreadCount();
  } catch (error) {
    console.error('标记已读失败:', error);
  }
};

const updateUnreadCount = () => {
  const count = alerts.value.reduce((total, group) => {
    return total + (group.alerts || []).filter(alert => !alert.isRead).length;
  }, 0);
  localStorage.setItem('alertUnreadCount', count.toString());
  window.dispatchEvent(new Event('alertCountUpdated'));
};

const handleViewDetail = async (alert) => {
  currentAlert.value = alert;
  showDetailDialog.value = true;
  if (!alert.isRead) {
    await markAsRead(alert.id);
  }
};

const handleMarkAllAsRead = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    await api.markAllAlertsAsRead(token);
    // 更新本地状态
    alerts.value = alerts.value.map(group => ({
      ...group,
      alerts: group.alerts.map(alert => ({
        ...alert,
        isRead: true
      }))
    }));
    updateUnreadCount();
    showSuccess('已全部标记为已读');
  } catch (error) {
    console.error('一键阅读失败:', error);
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
};

const loadAlerts = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    const result = await api.getAlertList({ role: 'COMPANY', companyId }, token);
    alerts.value = result || [];
    expandedGroups.value = alerts.value.map(g => g.batchId || 'unknown');
    updateUnreadCount();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadAlerts();
});
</script>

<style scoped>
.alert-center {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 28px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.alert-filter {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1;
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  padding: 10px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  min-width: 240px;
  background: white;
  transition: all 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.filter-select {
  padding: 10px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  min-width: 140px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.btn-refresh,
.btn-mark-all-read {
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.btn-refresh {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-refresh:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-mark-all-read {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.btn-mark-all-read:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
}

.btn-mark-all-read:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.btn-text {
  font-weight: 600;
}

.unread-badge-btn {
  font-size: 11px;
  font-weight: 700;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  padding: 2px 10px;
  border-radius: 20px;
  min-width: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.batch-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
}

.empty-state p {
  color: #64748b;
  font-size: 16px;
  margin: 0;
}

.batch-group {
  border-radius: 14px;
  overflow: hidden;
  background: white;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.batch-group:hover {
  box-shadow: 0 4px 12px 0 rgba(0, 0, 0, 0.1);
}

.batch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.3s ease;
}

.batch-header:hover {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-icon {
  font-size: 14px;
  color: #64748b;
  transition: transform 0.3s ease;
  width: 20px;
  text-align: center;
}

.collapse-icon.expanded {
  transform: rotate(-90deg);
}

.batch-icon {
  font-size: 20px;
}

.batch-name {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.batch-name-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  background: #e2e8f0;
  padding: 3px 10px;
  border-radius: 20px;
}

.batch-stats {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.stat-item {
  color: #64748b;
  font-weight: 500;
}

.stat-item.high-count {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
}

.stat-item.medium-count {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
}

.stat-item.low-count {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 10px;
  border-radius: 20px;
}

.alert-list {
  display: flex;
  flex-direction: column;
}

.alert-item {
  padding: 24px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid #f1f5f9;
}

.alert-item:last-child {
  border-bottom: none;
}

.alert-item:hover {
  background: linear-gradient(135deg, #fafafa 0%, #f8fafc 100%);
}

.alert-HIGH {
  border-left: 5px solid #ef4444;
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.03) 0%, transparent 100%);
}

.alert-MEDIUM {
  border-left: 5px solid #f59e0b;
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.03) 0%, transparent 100%);
}

.alert-LOW {
  border-left: 5px solid #3b82f6;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.03) 0%, transparent 100%);
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.alert-level {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
}

.unread-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
  animation: pulse 2s infinite;
}

.level-HIGH {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
  color: #dc2626;
}

.level-MEDIUM {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  color: #d97706;
}

.level-LOW {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  color: #2563eb;
}

.alert-status {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 20px;
  font-weight: 600;
}

.status-OPEN {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #475569;
  border: 1px solid #e2e8f0;
}

.status-CLOSED {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #059669;
  border: 1px solid #6ee7b7;
}

.alert-content {
  margin-bottom: 16px;
}

.alert-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.alert-description {
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 16px 0;
}

.alert-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #94a3b8;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f8fafc;
  padding: 6px 12px;
  border-radius: 8px;
}

.alert-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 16px;
  padding: 32px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dialog-content h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 28px 0;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.alert-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 28px;
}

.detail-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
}

.detail-item label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  min-width: 100px;
  flex-shrink: 0;
}

.detail-item span {
  font-size: 14px;
  color: #4b5563;
  flex: 1;
  word-break: break-all;
  font-weight: 500;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 28px;
}

@media (max-width: 768px) {
  .alert-center {
    padding: 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
  
  .alert-filter {
    flex-direction: column;
    gap: 16px;
  }
  
  .filter-left,
  .filter-right {
    width: 100%;
    flex-direction: column;
  }
  
  .search-input {
    min-width: 100%;
  }
  
  .alert-filter select {
    width: 100%;
  }
  
  .btn-mark-all-read,
  .btn-refresh {
    width: 100%;
    justify-content: center;
  }
  
  .batch-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .alert-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .alert-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .alert-actions {
    flex-direction: column;
  }
  
  .detail-item {
    flex-direction: column;
    gap: 8px;
  }
  
  .detail-item label {
    min-width: auto;
  }
}
</style>