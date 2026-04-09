<template>
  <Layout role="company">
    <div class="dashboard">
      <div class="dashboard-header">
        <h1 class="page-title">商家工作台</h1>
        <p class="page-subtitle">企业认证申请、系统消息与反馈查看</p>
      </div>

      <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error', success: noticeType === 'success' }">
        {{ notice }}
      </div>

      <div class="dashboard-cards">
        <div class="card">
          <div class="card-header">
            <h3>企业认证申请</h3>
          </div>
          <div class="form-group">
            <input v-model="applyForm.companyId" placeholder="companyId" />
            <input v-model="applyForm.companyName" placeholder="企业名称" />
            <input v-model="applyForm.remark" placeholder="申请备注" />
          </div>
          <div class="form-actions">
            <button class="btn-primary" :disabled="loading" @click="onApply">提交申请</button>
            <button class="btn-secondary" :disabled="loading" @click="onQueryStatus">查询认证状态</button>
          </div>
          <div class="status-info">
            状态：<span :class="{ 'status-pending': statusText.includes('PENDING'), 'status-approved': statusText.includes('APPROVED') }">
              {{ statusText }}
            </span>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3>系统消息</h3>
            <button class="btn-refresh" :disabled="loading" @click="loadMessages">
              <span v-if="loading" class="loading-spinner"></span>
              刷新消息
            </button>
          </div>
          <div class="table-wrapper">
            <table>
              <thead><tr><th>预警ID</th><th>二维码</th><th>等级</th><th>原因</th><th>动作结果</th><th>状态</th></tr></thead>
              <tbody>
              <tr v-if="messages.length === 0"><td colspan="6" class="empty-state">暂无消息</td></tr>
              <tr v-for="item in messages" :key="item.alertId">
                <td>{{ item.alertId }}</td>
                <td>{{ item.qsId }}</td>
                <td>
                  <span :class="{ 'level-high': item.alertLevel === 'HIGH', 'level-medium': item.alertLevel === 'MEDIUM' }">
                    {{ item.alertLevel }}
                  </span>
                </td>
                <td>{{ item.reason }}</td>
                <td>{{ item.actionResult }}</td>
                <td>{{ item.status }}</td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3>消费者反馈查看</h3>
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
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getMessages } from '../api/alert';
import { getFeedbackList } from '../api/feedback';
import { queryCompanyStatus, submitCompanyApply } from '../api/auth';
import { clearToken, getToken } from '../api/session';
import { useRouter } from 'vue-router';
import Layout from '../components/Layout.vue';

const router = useRouter();
const token = ref(getToken('company'));
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const applyForm = reactive({ companyId: '', companyName: '', remark: '' });
const statusText = ref('未查询');
const messages = ref([]);
const feedbacks = ref([]);

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
  if (message) {
    setTimeout(() => {
      notice.value = '';
    }, 3000);
  }
}

async function withLoading(task, successMessage = '') {
  loading.value = true;
  try {
    await task();
    if (successMessage) setNotice(successMessage, 'success');
  } catch (error) {
    if (error?.status === 401) {
      clearToken('company');
      router.push('/company/login');
      return;
    }
    setNotice(error?.message || '请求失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function onApply() {
  await withLoading(async () => {
    const res = await submitCompanyApply(applyForm, token.value);
    if (res.code === 200 && res.data) {
      statusText.value = res.data.statusText || 'PENDING';
    }
  }, '认证申请已提交');
}

async function onQueryStatus() {
  if (!applyForm.companyId) {
    setNotice('请先填写 companyId', 'error');
    return;
  }
  await withLoading(async () => {
    const res = await queryCompanyStatus(applyForm.companyId, token.value);
    if (res.code === 200 && res.data) {
      statusText.value = `${res.data.statusText || ''} ${res.data.remark || ''}`.trim();
    }
  });
}

async function loadMessages() {
  const res = await getMessages(token.value);
  messages.value = res.data || [];
}

async function loadFeedbacks() {
  const res = await getFeedbackList(token.value);
  feedbacks.value = res.data || [];
}

onMounted(async () => {
  await withLoading(async () => {
    await Promise.all([loadMessages(), loadFeedbacks()]);
  });
});
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.dashboard-header {
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

.status-line {
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.status-line.error {
  background-color: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.status-line.success {
  background-color: #d1fae5;
  color: #065f46;
  border: 1px solid #bbf7d0;
}

.dashboard-cards {
  display: flex;
  flex-direction: column;
  gap: 24px;
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

.form-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-primary {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  padding: 10px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-info {
  font-size: 14px;
  color: #64748b;
}

.status-pending {
  color: #f59e0b;
  font-weight: 600;
}

.status-approved {
  color: #10b981;
  font-weight: 600;
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
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
}
</style>

