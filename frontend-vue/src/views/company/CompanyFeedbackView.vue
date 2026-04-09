<template>
  <Layout role="company">
    <div class="feedback-view">
      <div class="page-header">
        <h1 class="page-title">消费者反馈查看</h1>
        <p class="page-subtitle">查看消费者提交的反馈信息</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>反馈列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadFeedbacks">
            <span v-if="loading" class="loading-spinner"></span>
            刷新反馈
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>反馈编号</th><th>二维码</th><th>类型</th><th>地区</th><th>风险</th><th>状态</th><th>提交IP</th></tr></thead>
            <tbody>
            <tr v-if="feedbacks.length === 0"><td colspan="7" class="empty-state">暂无反馈数据</td></tr>
            <tr v-for="item in feedbacks" :key="item.feedbackId">
              <td>{{ item.feedbackId }}</td>
              <td>{{ item.qsId }}</td>
              <td>{{ item.feedbackType }}</td>
              <td>{{ item.region }}</td>
              <td>{{ item.riskLevel }}</td>
              <td>
                <span class="status-badge">{{ item.status }}</span>
              </td>
              <td>{{ item.submitterIpMasked || '-' }}</td>
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
import { getFeedbackList } from '../../api/feedback';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const feedbacks = ref([]);
const token = ref(getToken('company'));

async function loadFeedbacks() {
  loading.value = true;
  try {
    const res = await getFeedbackList(token.value);
    feedbacks.value = res.data || [];
  } catch (error) {
    console.error('加载反馈列表失败:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadFeedbacks();
});
</script>

<style scoped>
.feedback-view {
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

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  background: #f1f5f9;
  font-size: 12px;
  color: #64748b;
  border: 1px solid #e2e8f0;
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