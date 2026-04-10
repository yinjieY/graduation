<template>
  <Layout :role="'company'">
    <div class="alert-center">
      <h1 class="page-title">预警中心</h1>
      
      <div class="alert-filter">
        <select v-model="filterLevel" @change="loadAlerts">
          <option value="">全部级别</option>
          <option value="HIGH">高风险</option>
          <option value="MEDIUM">中风险</option>
          <option value="LOW">低风险</option>
        </select>
        <select v-model="filterStatus" @change="loadAlerts">
          <option value="">全部状态</option>
          <option value="OPEN">未闭环</option>
          <option value="CLOSED">已闭环</option>
        </select>
      </div>
      
      <div class="alert-list">
        <div class="alert-item" v-for="alert in alerts" :key="alert.id" :class="`alert-${alert.level}`">
          <div class="alert-header">
            <div class="alert-level" :class="`level-${alert.level}`">
              {{ alert.level === 'HIGH' ? '高风险' : alert.level === 'MEDIUM' ? '中风险' : '低风险' }}
            </div>
            <div class="alert-status" :class="`status-${alert.status}`">
              {{ alert.status === 'OPEN' ? '未闭环' : '已闭环' }}
            </div>
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
      
      <!-- 预警详情对话框 -->
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
import { ref, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';

const api = useApi();
const { showError } = useNotification();

const loading = ref(false);
const showDetailDialog = ref(false);
const filterLevel = ref('');
const filterStatus = ref('');
const currentAlert = ref({});
const alerts = ref([]);

function mapAlert(item) {
  return {
    id: item.alertId,
    level: String(item.alertLevel || 'LOW').toUpperCase(),
    status: String(item.status || 'OPEN').toUpperCase(),
    title: item.reason || '风险预警',
    description: item.detail || '-',
    qsId: item.qsId || '-',
    createdAt: item.createdAt || '-',
    processedAt: item.status === 'CLOSED' ? item.createdAt : '',
    processNote: item.actionResult || ''
  };
}

const handleViewDetail = (alert) => {
  currentAlert.value = alert;
  showDetailDialog.value = true;
};


const loadAlerts = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const rows = await api.getAlertList({}, token);
    alerts.value = (rows || [])
      .map(mapAlert)
      .filter((item) => !filterLevel.value || item.level === filterLevel.value)
      .filter((item) => !filterStatus.value || item.status === filterStatus.value);
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
}

.alert-filter select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 150px;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.alert-item {
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.alert-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
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
}

.alert-level {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
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

/* 对话框样式 */
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
  min-width: 80px;
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