<template>
  <Layout role="admin">
    <div class="feedback-management">
      <div class="page-header">
        <h1 class="page-title">反馈处理</h1>
        <p class="page-subtitle">处理消费者反馈和投诉</p>
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
            <thead><tr><th>反馈编号</th><th>二维码</th><th>企业</th><th>类型</th><th>风险</th><th>状态</th><th>提交IP</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="feedbacks.length === 0"><td colspan="8" class="empty-state">暂无反馈数据</td></tr>
            <tr v-for="item in feedbacks" :key="item.feedbackId">
              <td>{{ item.feedbackId }}</td>
              <td>{{ item.qsId }}</td>
              <td>{{ item.companyId }}</td>
              <td>{{ item.feedbackType }}</td>
              <td>{{ item.riskLevel }}</td>
              <td>
                <span class="status-badge">{{ item.status }}</span>
              </td>
              <td>{{ item.submitterIpMasked || '-' }}</td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="loadDetail(item.feedbackId)">详情</button>
                <button class="btn-secondary" :disabled="loading" @click="updateStatus(item.feedbackId, 'ACCEPTED')">受理</button>
                <button class="btn-secondary" :disabled="loading" @click="updateStatus(item.feedbackId, 'REJECTED')">驳回</button>
                <button class="btn-danger" :disabled="loading" @click="updateStatus(item.feedbackId, 'CLOSED')">结案</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card detail-card">
        <div class="card-header">
          <h3>反馈详情与处置</h3>
        </div>
        <div class="detail-header">
          <input v-model="detailForm.feedbackId" placeholder="请输入反馈编号查阅" />
          <button class="btn-primary" :disabled="loading" @click="loadDetail(detailForm.feedbackId)">加载详情</button>
        </div>
        <textarea v-model="detailForm.handleNote" placeholder="处理备注（结案时将附带 submitterIp + 当前二维码状态）"></textarea>
        <div v-if="Object.keys(detailData).length > 0" class="detail-data">
          <pre>{{ JSON.stringify(detailData, null, 2) }}</pre>
        </div>
        <div v-else class="empty-detail">
          请选择或输入反馈编号加载详情数据
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getFeedbackDetail, getFeedbackList, updateFeedbackStatus } from '../../api/feedback';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const feedbacks = ref([]);
const detailData = ref({});
const detailForm = reactive({ feedbackId: '', handleNote: '' });
const token = ref(getToken('admin'));

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

async function loadDetail(feedbackId) {
  if (!feedbackId) return;
  loading.value = true;
  try {
    const res = await getFeedbackDetail(feedbackId, token.value);
    detailData.value = res.data || {};
    detailForm.feedbackId = feedbackId;
  } catch (error) {
    console.error('加载反馈详情失败:', error);
  } finally {
    loading.value = false;
  }
}

async function updateStatus(feedbackId, status) {
  if (!feedbackId) return;
  loading.value = true;
  try {
    await updateFeedbackStatus(feedbackId, status, detailForm.handleNote, token.value);
    await loadFeedbacks();
    await loadDetail(feedbackId);
  } catch (error) {
    console.error('更新反馈状态失败:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadFeedbacks();
});
</script>

<style scoped>
.feedback-management {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  margin-bottom: 8px;
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

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.btn-primary {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-danger {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #ef4444;
  color: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.detail-card {
  margin-top: 0;
}

.detail-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.detail-header input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
}

.detail-header input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  min-height: 100px;
  margin-bottom: 16px;
}

textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.detail-data {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}

.detail-data pre {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  color: #334155;
}

.empty-detail {
  padding: 48px 24px;
  text-align: center;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-style: italic;
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
  
  .actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>