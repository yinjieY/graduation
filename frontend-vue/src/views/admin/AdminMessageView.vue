<template>
  <Layout role="admin">
    <div class="message-management">
      <div class="page-header">
        <h1 class="page-title">系统消息</h1>
        <p class="page-subtitle">查看系统预警和通知</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>系统消息列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadMessages">
            <span v-if="loading" class="loading-spinner"></span>
            刷新消息
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>预警ID</th><th>企业</th><th>等级</th><th>原因</th><th>状态</th><th>动作结果</th></tr></thead>
            <tbody>
            <tr v-if="messages.length === 0"><td colspan="6" class="empty-state">暂无系统消息</td></tr>
            <tr v-for="item in messages" :key="item.alertId">
              <td>{{ item.alertId }}</td>
              <td>{{ item.companyId }}</td>
              <td>
                <span :class="{ 'level-high': item.alertLevel === 'HIGH', 'level-medium': item.alertLevel === 'MEDIUM' }">
                  {{ item.alertLevel }}
                </span>
              </td>
              <td>{{ item.reason }}</td>
              <td>{{ item.status }}</td>
              <td>{{ item.actionResult }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getMessages } from '../../api/alert';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const messages = ref([]);
const token = ref(getToken('admin'));

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
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin: 0;
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

.level-high {
  color: #ef4444;
  font-weight: 600;
}

.level-medium {
  color: #3b82f6;
  font-weight: 600;
}

@media (max-width: 768px) {
  .card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>