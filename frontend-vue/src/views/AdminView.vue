<template>
  <Layout role="admin">
    <div class="dashboard">
      <div class="dashboard-header">
        <h1 class="page-title">管理员工作台</h1>
        <p class="page-subtitle">企业审查、系统消息与消费者反馈处理</p>
      </div>

      <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error', success: noticeType === 'success' }">
        {{ notice }}
      </div>

      <div class="dashboard-cards">
        <div class="card">
          <div class="card-header">
            <h3>企业审查</h3>
            <button class="btn-refresh" :disabled="loading" @click="loadPending">
              <span v-if="loading" class="loading-spinner"></span>
              刷新
            </button>
          </div>
          <div class="table-wrapper">
            <table>
              <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>备注</th><th>操作</th></tr></thead>
              <tbody>
              <tr v-if="pendingList.length === 0"><td colspan="5" class="empty-state">暂无待审企业</td></tr>
              <tr v-for="item in pendingList" :key="item.companyId">
                <td>{{ item.companyId }}</td>
                <td class="company-name">{{ item.companyName }}</td>
                <td>{{ item.applyBy }}</td>
                <td class="remark">{{ item.remark }}</td>
                <td class="actions">
                  <button class="btn-primary" :disabled="loading" @click="review(item.companyId, true)">通过</button>
                  <button class="btn-secondary" :disabled="loading" @click="review(item.companyId, false)">拒绝</button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3>系统消息</h3>
            <button class="btn-refresh" :disabled="loading" @click="loadMessages">
              <span v-if="loading" class="loading-spinner"></span>
              刷新
            </button>
          </div>
          <div class="batch-groups">
            <div v-if="filteredMessages.length === 0" class="empty-state">暂无系统消息</div>
            
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
                    <table>
                      <thead><tr><th>预警ID</th><th>企业</th><th>等级</th><th>原因</th><th>状态</th><th>动作结果</th></tr></thead>
                      <tbody>
                      <tr v-for="item in qsGroup.alerts" :key="item.alertId" :class="{ highlight: query.alertId && String(item.alertId) === String(query.alertId) }">
                        <td>{{ item.alertId }}</td>
                        <td>{{ item.companyId }}</td>
                        <td>
                          <span :class="{ 'level-high': item.alertLevel === 'HIGH' || item.alertLevel === 'CRITICAL', 'level-medium': item.alertLevel === 'MEDIUM' }">
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
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3>个人信息</h3>
            <button class="btn-refresh" :disabled="loading" @click="loadUserInfo">
              <span v-if="loading" class="loading-spinner"></span>
              刷新
            </button>
          </div>
          <div class="user-info-form">
            <div class="form-row">
              <label>用户名</label>
              <input v-model="userInfoForm.username" disabled class="disabled-input" />
            </div>
            <div class="form-row">
              <label>角色</label>
              <input v-model="userInfoForm.role" disabled class="disabled-input" />
            </div>
            <div class="form-row">
              <label>邮箱 <span class="required">*</span></label>
              <input v-model="userInfoForm.email" type="email" placeholder="请输入邮箱地址" />
            </div>
            <div class="form-row">
              <label>手机号</label>
              <input v-model="userInfoForm.phone" type="tel" placeholder="请输入手机号" />
            </div>
            <div class="form-actions">
              <button class="btn-primary" :disabled="loading" @click="handleUpdateUserInfo">保存修改</button>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h3>反馈处理</h3>
            <button class="btn-refresh" :disabled="loading" @click="loadFeedbacks">
              <span v-if="loading" class="loading-spinner"></span>
              刷新
            </button>
          </div>
          <div class="table-wrapper">
            <table>
              <thead><tr><th>反馈编号</th><th>二维码</th><th>企业</th><th>类型</th><th>风险</th><th>状态</th><th>提交IP</th><th>操作</th></tr></thead>
              <tbody>
              <tr v-if="filteredFeedbacks.length === 0"><td colspan="8" class="empty-state">暂无反馈数据</td></tr>
              <tr v-for="item in filteredFeedbacks" :key="item.feedbackId" :class="{ highlight: query.feedbackId && String(item.feedbackId) === String(query.feedbackId) }">
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

          <div class="detail-section">
            <h3>反馈详情与处置</h3>
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
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getMessages } from '../api/alert';
import { getFeedbackDetail, getFeedbackList, updateFeedbackStatus } from '../api/feedback';
import { getPending, reviewCompany, getUserInfo, updateUserInfo } from '../api/auth';
import { clearToken, getToken } from '../api/session';
import Layout from '../components/Layout.vue';

const route = useRoute();
const router = useRouter();
const token = ref(getToken('admin'));
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const detailForm = reactive({ feedbackId: '', handleNote: '' });
const pendingList = ref([]);
const messageList = ref([]);
const feedbackList = ref([]);
const detailData = ref({});
const userInfoForm = reactive({ username: '', email: '', phone: '', role: '' });
const collapsedQsIds = ref(new Set());

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

const query = computed(() => ({
  alertId: route.query.alertId || route.query.eventId || '',
  qsId: route.query.qsId || '',
  companyId: route.query.companyId || '',
  feedbackId: route.query.feedbackId || ''
}));

const filteredMessages = computed(() => messageList.value.filter((group) => {
  // 如果有查询参数，先检查该批次内是否有匹配的预警
  if (query.value.alertId || query.value.companyId) {
    const hasMatchingAlert = (group.alerts || []).some(item => {
      if (query.value.alertId && String(item.alertId) !== String(query.value.alertId)) return false;
      if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
      return true;
    });
    if (!hasMatchingAlert) return false;
  }
  return true;
}).map(group => ({
  ...group,
  alerts: (group.alerts || []).filter(item => {
    if (query.value.alertId && String(item.alertId) !== String(query.value.alertId)) return false;
    if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
    return true;
  })
})).filter(group => group.alerts && group.alerts.length > 0));

const filteredFeedbacks = computed(() => feedbackList.value.filter((item) => {
  if (query.value.qsId && String(item.qsId) !== String(query.value.qsId)) return false;
  if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
  if (query.value.feedbackId && String(item.feedbackId) !== String(query.value.feedbackId)) return false;
  return true;
}));

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
      clearToken('admin');
      router.push('/admin/login');
      return;
    }
    setNotice(error?.message || '请求失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function loadPending() {
  const res = await getPending(token.value);
  pendingList.value = res.data || [];
}

async function review(companyId, approved) {
  await withLoading(async () => {
    await reviewCompany(companyId, approved, token.value);
    await loadPending();
  }, approved ? '已通过企业申请' : '已拒绝企业申请');
}

async function loadMessages() {
  const res = await getMessages(token.value);
  console.log('loadMessages 原始响应:', res);
  messageList.value = res.data || [];
}

async function loadFeedbacks() {
  const res = await getFeedbackList(token.value);
  feedbackList.value = res.data || [];
}

async function loadDetail(feedbackId) {
  if (!feedbackId) return;
  const res = await getFeedbackDetail(feedbackId, token.value);
  detailData.value = res.data || {};
  detailForm.feedbackId = feedbackId;
}

async function updateStatus(feedbackId, status) {
  if (!feedbackId) return;
  await withLoading(async () => {
    await updateFeedbackStatus(feedbackId, status, detailForm.handleNote, token.value);
    await loadFeedbacks();
    await loadDetail(feedbackId);
  }, `反馈 ${feedbackId} 已更新为 ${status}`);
}

async function loadUserInfo() {
  const res = await getUserInfo(token.value);
  Object.assign(userInfoForm, res.data);
}

async function handleUpdateUserInfo() {
  await withLoading(async () => {
    await updateUserInfo(userInfoForm.email, userInfoForm.phone, token.value);
    await loadUserInfo();
  }, '个人信息更新成功');
}

onMounted(async () => {
  await withLoading(async () => {
    await Promise.all([loadPending(), loadMessages(), loadFeedbacks(), loadUserInfo()]);
    if (query.value.feedbackId) {
      await loadDetail(String(query.value.feedbackId));
    }
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

.alert-list {
  overflow-x: auto;
}

.alert-list table {
  margin-bottom: 0;
}

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

.company-name {
  font-weight: 500;
  color: #1e293b;
}

.remark {
  max-width: 200px;
  white-space: normal;
  word-break: break-word;
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

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
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

.highlight {
  background-color: rgba(59, 130, 246, 0.05);
}

.detail-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px dashed #e2e8f0;
}

.detail-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 16px 0;
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

.user-info-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-row label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.form-row .required {
  color: #ef4444;
}

.form-row input {
  padding: 10px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-row input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-row .disabled-input {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
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