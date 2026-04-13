<template>
  <Layout :role="'company'">
    <div class="alert-center">
      <h1 class="page-title">预警中心</h1>
      
      <div class="alert-filter">
        <input 
          v-model="searchKeyword" 
          type="text" 
          placeholder="搜索批次ID或批次名称" 
          class="search-input"
          @input="handleSearch"
        />
        <select v-model="filterLevel">
          <option value="">全部级别</option>
          <option value="HIGH">高风险</option>
          <option value="MEDIUM">中风险</option>
          <option value="LOW">低风险</option>
        </select>
        <select v-model="filterStatus">
          <option value="">全部状态</option>
          <option value="OPEN">未闭环</option>
          <option value="CLOSED">已闭环</option>
        </select>
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
      alerts: group.alerts.map(alert => 
        alert.alertId === alertId ? { ...alert, isRead: true } : alert
      )
    }));
    updateUnreadCount();
  } catch (error) {
    console.error('标记已读失败:', error);
  }
};

const updateUnreadCount = () => {
  const unreadCount = alerts.value.reduce((total, group) => {
    return total + group.alerts.filter(alert => !alert.isRead).length;
  }, 0);
  localStorage.setItem('alertUnreadCount', unreadCount.toString());
  window.dispatchEvent(new Event('alertCountUpdated'));
};

const handleViewDetail = async (alert) => {
  currentAlert.value = alert;
  showDetailDialog.value = true;
  if (!alert.isRead) {
    await markAsRead(alert.id);
  }
};

const handleSearch = () => {
};

const loadAlerts = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const result = await api.getAlertList({}, token);
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
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.alert-filter {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  align-items: center;
}

.alert-filter .search-input {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 200px;
  flex: 1;
  max-width: 300px;
}

.alert-filter select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 150px;
}

.batch-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
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

.batch-header {
  cursor: pointer;
}

.batch-header:hover {
  background: #f1f5f9;
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-icon {
  font-size: 12px;
  color: #64748b;
  transition: transform 0.2s ease;
  width: 16px;
  text-align: center;
}

.collapse-icon.expanded {
  transform: rotate(-90deg);
}

.batch-icon {
  font-size: 18px;
}

.batch-name {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.batch-name-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 400;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.batch-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.stat-item {
  color: #64748b;
}

.stat-item.high-count {
  color: #ef4444;
  font-weight: 500;
}

.stat-item.medium-count {
  color: #f59e0b;
  font-weight: 500;
}

.stat-item.low-count {
  color: #3b82f6;
  font-weight: 500;
}

.alert-list {
  display: flex;
  flex-direction: column;
}

.alert-item {
  padding: 20px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid #f1f5f9;
}

.alert-item:last-child {
  border-bottom: none;
}

.alert-item:hover {
  background: #fafafa;
}

.alert-HIGH {
  border-left: 4px solid #ef4444;
}

.alert-MEDIUM {
  border-left: 4px solid #f59e0b;
}

.alert-LOW {
  border-left: 4px solid #3b82f6;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 8px;
}

.alert-level {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.unread-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #ef4444;
  color: white;
  font-weight: 500;
}

.level-HIGH {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.level-MEDIUM {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}

.level-LOW {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.alert-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.status-OPEN {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
}

.status-CLOSED {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.alert-content {
  margin-bottom: 16px;
}

.alert-title {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 8px 0;
}

.alert-description {
  font-size: 14px;
  color: #64748b;
  line-height: 1.4;
  margin: 0 0 12px 0;
}

.alert-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.alert-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.dialog-content h2 {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
  margin: 0 0 24px 0;
}

.alert-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.detail-item label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  min-width: 100px;
  flex-shrink: 0;
}

.detail-item span {
  font-size: 14px;
  color: #64748b;
  flex: 1;
  word-break: break-all;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
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
    gap: 8px;
  }
  
  .alert-filter select {
    width: 100%;
  }
  
  .batch-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .alert-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .alert-meta {
    flex-direction: column;
    gap: 4px;
  }
  
  .alert-actions {
    flex-direction: column;
  }
  
  .detail-item {
    flex-direction: column;
    gap: 4px;
  }
  
  .detail-item label {
    min-width: auto;
  }
}
</style>