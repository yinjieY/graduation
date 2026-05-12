<template>
  <Layout role="admin">
    <div class="feedback-management">
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">反馈处理</h1>
          <p class="page-subtitle">处理消费者反馈和投诉</p>
        </div>
        <div class="header-actions">
          <button class="btn-refresh" :disabled="loading" @click="loadFeedbacks">
            <span v-if="loading" class="loading-spinner"></span>
            <span v-else>⟳</span>
            刷新
          </button>
        </div>
      </div>

      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">全部反馈</div>
        </div>
        <div class="stat-card pending">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </div>
        <div class="stat-card accepted">
          <div class="stat-value">{{ stats.accepted }}</div>
          <div class="stat-label">已受理</div>
        </div>
        <div class="stat-card high-risk">
          <div class="stat-value">{{ stats.highRisk }}</div>
          <div class="stat-label">高风险</div>
        </div>
      </div>

      <div class="card filter-section">
        <div class="filter-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            :class="['filter-tab', { active: currentStatus === tab.value }]"
            @click="currentStatus = tab.value"
          >
            {{ tab.label }}
            <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
          </button>
        </div>
        <div class="filter-row">
          <select v-model="currentRiskLevel" class="filter-select">
            <option value="">全部风险等级</option>
            <option value="HIGH">高风险</option>
            <option value="MEDIUM">中风险</option>
            <option value="LOW">低风险</option>
          </select>
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索反馈编号、二维码、企业..."
              @keyup.enter="handleSearch"
            />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>序号</th>
                <th>反馈编号</th>
                <th>二维码</th>
                <th>企业</th>
                <th>类型</th>
                <th>风险</th>
                <th>状态</th>
                <th>提交时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredFeedbacks.length === 0">
                <td colspan="9" class="empty-state">暂无反馈数据</td>
              </tr>
              <tr v-for="(item, index) in filteredFeedbacks" :key="item.feedbackId">
                <td>{{ index + 1 }}</td>
                <td class="feedback-id">{{ item.feedbackId }}</td>
                <td class="qs-id">{{ item.qsId }}</td>
                <td>{{ item.companyId || '-' }}</td>
                <td>{{ item.feedbackType || '-' }}</td>
                <td>
                  <span :class="['risk-badge', `risk-${item.riskLevel?.toLowerCase()}`]">
                    {{ formatRiskLevel(item.riskLevel) }}
                  </span>
                </td>
                <td>
                  <span :class="['status-badge', `status-${item.status?.toLowerCase()}`]">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
                <td>{{ formatTime(item.createTime) }}</td>
                <td>
                  <button class="btn-handle" @click="openPanel(item)">处理</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-if="selectedFeedback" :class="['slide-panel', { open: panelOpen }]">
        <div class="panel-overlay" @click="closePanel"></div>
        <div class="panel-content">
          <div class="panel-header">
            <h2>反馈处置</h2>
            <button class="btn-close" @click="closePanel">×</button>
          </div>

          <div class="panel-body">
            <div v-if="loadingDetail" class="loading-detail">
              <span class="loading-spinner"></span>
            </div>
            <template v-else>
              <div class="info-section">
                <h3>基本信息</h3>
              <div class="info-grid">
                <div class="info-item">
                  <label>反馈编号</label>
                  <span class="info-value">{{ selectedFeedback.feedbackId }}</span>
                </div>
                <div class="info-item">
                  <label>二维码</label>
                  <span class="info-value">{{ selectedFeedback.qsId }}</span>
                </div>
                <div class="info-item">
                  <label>企业</label>
                  <span class="info-value">{{ selectedFeedback.companyId || '-' }}</span>
                </div>
                <div class="info-item">
                  <label>反馈类型</label>
                  <span class="info-value">{{ selectedFeedback.feedbackType || '-' }}</span>
                </div>
                <div class="info-item">
                  <label>风险等级</label>
                  <span :class="['risk-badge', `risk-${selectedFeedback.riskLevel?.toLowerCase()}`]">
                    {{ formatRiskLevel(selectedFeedback.riskLevel) }}
                  </span>
                </div>
                <div class="info-item">
                  <label>提交时间</label>
                  <span class="info-value">{{ formatTime(selectedFeedback.createTime) }}</span>
                </div>
              </div>
            </div>

            <div v-if="selectedFeedback.submitterIpMasked" class="info-section">
              <h3>提交者信息</h3>
              <div class="info-grid">
                <div class="info-item">
                  <label>提交者IP</label>
                  <span class="info-value">{{ selectedFeedback.submitterIpMasked }}</span>
                </div>
              </div>
            </div>

            <div v-if="selectedFeedback.handleRecords && selectedFeedback.handleRecords.length > 0" class="info-section">
              <h3>处理历史</h3>
              <div class="timeline">
                <div v-for="(record, index) in selectedFeedback.handleRecords" :key="index" class="timeline-item">
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <div class="timeline-header">
                      <span :class="['status-badge', `status-${record.status?.toLowerCase()}`]">{{ formatStatus(record.status) }}</span>
                      <span class="timeline-time">{{ formatTime(record.handleTime) }}</span>
                    </div>
                    <div v-if="record.handler" class="timeline-handler">处理人: {{ record.handler }}</div>
                    <div v-if="record.handleNote" class="timeline-note">{{ record.handleNote }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div class="info-section">
              <h3>处置操作</h3>
              <div class="status-selector">
                <label
                  v-for="option in statusOptions"
                  :key="option.value"
                  :class="['status-option', { selected: handleForm.status === option.value }]"
                >
                  <input
                    type="radio"
                    :value="option.value"
                    v-model="handleForm.status"
                    name="handleStatus"
                  />
                  <span :class="['option-badge', `status-${option.value.toLowerCase()}`]">
                    {{ option.label }}
                  </span>
                </label>
              </div>

              <div class="handle-options">
                <label class="checkbox-item">
                  <input type="checkbox" v-model="handleForm.freezeQrcode" />
                  <span>冻结相关二维码</span>
                </label>
                <label class="checkbox-item">
                  <input type="checkbox" v-model="handleForm.notifyCompany" />
                  <span>同步通知企业</span>
                </label>
              </div>

              <div class="note-section">
                <label>处理备注</label>
                <textarea
                  v-model="handleForm.handleNote"
                  placeholder="请输入处理备注（选填）..."
                  rows="4"
                ></textarea>
              </div>
            </div>

            <div class="panel-footer">
            <button class="btn-cancel" @click="closePanel">取消</button>
            <button
              class="btn-submit"
              :disabled="!handleForm.status || submitting"
              @click="submitHandle"
            >
              <span v-if="submitting" class="loading-spinner"></span>
              {{ submitting ? '提交中...' : '确认处置' }}
            </button>
          </div>
          </template>
        </div>
      </div>
      </div>

      <div v-if="confirmDialog.show" class="confirm-dialog">
        <div class="dialog-overlay" @click="confirmDialog.show = false"></div>
        <div class="dialog-content">
          <h3>确认处置</h3>
          <p>确定要将该反馈状态更新为「{{ formatStatus(confirmDialog.status) }}」吗？</p>
          <div class="dialog-footer">
            <button class="btn-cancel" @click="confirmDialog.show = false">取消</button>
            <button class="btn-confirm" @click="executeHandle">确定</button>
          </div>
        </div>
      </div>

      <div v-if="toast.show" :class="['toast', `toast-${toast.type}`]">
        {{ toast.message }}
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref, reactive, computed, watch } from 'vue';
import { getFeedbackDetail, getFeedbackList, updateFeedbackStatus } from '../../api/feedback';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const loadingDetail = ref(false);
const submitting = ref(false);
const feedbacks = ref([]);
const selectedFeedback = ref(null);
const panelOpen = ref(false);
const token = ref(getToken('admin'));

const currentStatus = ref('');
const currentRiskLevel = ref('');
const searchKeyword = ref('');

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '已受理', value: 'ACCEPTED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已结案', value: 'CLOSED' }
];

const statusOptions = [
  { label: '已受理', value: 'ACCEPTED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已结案', value: 'CLOSED' }
];

const handleForm = reactive({
  status: '',
  handleNote: '',
  freezeQrcode: false,
  notifyCompany: false
});

const confirmDialog = reactive({
  show: false,
  status: ''
});

const toast = reactive({
  show: false,
  message: '',
  type: 'success'
});

const stats = computed(() => {
  const list = feedbacks.value;
  return {
    total: list.length,
    pending: list.filter(f => f.status === 'PENDING').length,
    accepted: list.filter(f => f.status === 'ACCEPTED').length,
    highRisk: list.filter(f => f.riskLevel === 'HIGH').length
  };
});

const filteredFeedbacks = computed(() => {
  let result = feedbacks.value;

  if (currentStatus.value) {
    result = result.filter(f => f.status === currentStatus.value);
  }

  if (currentRiskLevel.value) {
    result = result.filter(f => f.riskLevel === currentRiskLevel.value);
  }

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    result = result.filter(f =>
      (f.feedbackId && f.feedbackId.toLowerCase().includes(keyword)) ||
      (f.qsId && f.qsId.toLowerCase().includes(keyword)) ||
      (f.companyId && f.companyId.toLowerCase().includes(keyword))
    );
  }

  return result;
});

watch(currentStatus, () => {
  statusTabs[0].count = undefined;
});

function formatRiskLevel(level) {
  const map = { HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险', null: '-' };
  return map[level] || level || '-';
}

function formatStatus(status) {
  const map = { PENDING: '待处理', ACCEPTED: '已受理', REJECTED: '已驳回', CLOSED: '已结案', null: '-' };
  return map[status] || status || '-';
}

function formatTime(time) {
  if (!time) return '-';
  try {
    let date;
    if (typeof time === 'number') {
      date = new Date(time);
    } else if (typeof time === 'string') {
      if (time.length === 13 && !isNaN(time)) {
        date = new Date(parseInt(time));
      } else {
        date = new Date(time);
      }
    } else {
      return '-';
    }
    if (isNaN(date.getTime())) return '-';
    return date.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch {
    return '-';
  }
}

async function loadFeedbacks() {
  loading.value = true;
  try {
    const res = await getFeedbackList(token.value);
    feedbacks.value = res.data || [];
  } catch (error) {
    console.error('加载反馈列表失败:', error);
    showToast('加载失败: ' + error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function openPanel(item) {
  loadingDetail.value = true;
  try {
    const detail = await getFeedbackDetail(item.feedbackId, token.value);
    selectedFeedback.value = detail.data || item;
  } catch (error) {
    console.error('获取反馈详情失败:', error);
    selectedFeedback.value = item;
  } finally {
    loadingDetail.value = false;
  }
  handleForm.status = '';
  handleForm.handleNote = '';
  handleForm.freezeQrcode = false;
  handleForm.notifyCompany = false;
  panelOpen.value = true;
}

function closePanel() {
  panelOpen.value = false;
  setTimeout(() => {
    selectedFeedback.value = null;
  }, 300);
}

function handleSearch() {
}

function submitHandle() {
  if (!handleForm.status) {
    showToast('请选择处置状态', 'error');
    return;
  }
  confirmDialog.status = handleForm.status;
  confirmDialog.show = true;
}

async function executeHandle() {
  confirmDialog.show = false;
  submitting.value = true;
  try {
    const resp = await updateFeedbackStatus(
      selectedFeedback.value.feedbackId,
      handleForm.status,
      handleForm.handleNote,
      handleForm.freezeQrcode,
      handleForm.notifyCompany,
      token.value
    );
    
    let msg = '处置成功';
    if (resp.data) {
      if (handleForm.freezeQrcode && resp.data.freezeSuccess !== undefined) {
        msg += resp.data.freezeSuccess ? '，二维码已冻结' : '，二维码冻结失败';
      }
      if (handleForm.notifyCompany && resp.data.notifySuccess !== undefined) {
        msg += resp.data.notifySuccess ? '，企业已通知' : '，企业通知失败';
      }
    }
    showToast(msg, 'success');
    closePanel();
    await loadFeedbacks();
  } catch (error) {
    console.error('处置失败:', error);
    showToast('处置失败: ' + error.message, 'error');
  } finally {
    submitting.value = false;
  }
}

function showToast(message, type = 'success') {
  toast.message = message;
  toast.type = type;
  toast.show = true;
  setTimeout(() => {
    toast.show = false;
  }, 3000);
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
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left: 4px solid #94a3b8;
}

.stat-card.pending { border-left-color: #f59e0b; }
.stat-card.accepted { border-left-color: #3b82f6; }
.stat-card.high-risk { border-left-color: #ef4444; }

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 6px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.filter-tab.active {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.tab-count {
  background: rgba(255,255,255,0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.filter-tab.active .tab-count {
  background: rgba(255,255,255,0.3);
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  color: #334155;
  cursor: pointer;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fafafa;
}

.search-box:focus-within {
  border-color: #3b82f6;
  background: white;
}

.search-icon {
  font-size: 14px;
  opacity: 0.5;
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: #334155;
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
  white-space: nowrap;
}

td {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  color: #475569;
}

.feedback-id, .qs-id {
  font-family: monospace;
  font-size: 13px;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 48px 24px;
}

.risk-badge, .status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.risk-high, .risk-high { background: #fef2f2; color: #dc2626; }
.risk-medium, .risk-medium { background: #fffbeb; color: #d97706; }
.risk-low, .risk-low { background: #f0fdf4; color: #16a34a; }
.risk-null { background: #f1f5f9; color: #64748b; }

.status-pending, .status-pending { background: #fff7ed; color: #ea580c; }
.status-accepted, .status-accepted { background: #eff6ff; color: #2563eb; }
.status-rejected, .status-rejected { background: #f1f5f9; color: #64748b; }
.status-closed, .status-closed { background: #f0fdf4; color: #16a34a; }
.status-null, .status-null { background: #f1f5f9; color: #64748b; }

.btn-handle {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-handle:hover {
  background: #2563eb;
}

.slide-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 480px;
  height: 100vh;
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}

.slide-panel.open {
  pointer-events: auto;
  opacity: 1;
}

.panel-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
}

.panel-content {
  position: relative;
  width: 100%;
  max-width: 480px;
  height: 100%;
  background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.3s ease;
}

.slide-panel.open .panel-content {
  transform: translateX(0);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.panel-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.btn-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-close:hover {
  background: #e2e8f0;
  color: #334155;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.loading-detail {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.loading-detail .loading-spinner {
  width: 24px;
  height: 24px;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

.timeline-item {
  position: relative;
  margin-bottom: 16px;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: -18px;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
  border: 2px solid white;
  box-shadow: 0 0 0 2px #e2e8f0;
}

.timeline-content {
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.timeline-time {
  font-size: 12px;
  color: #94a3b8;
}

.timeline-handler {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 4px;
}

.timeline-note {
  font-size: 13px;
  color: #475569;
  padding: 8px;
  background: white;
  border-radius: 4px;
  margin-top: 4px;
}

.info-section h3 {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 12px 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: #94a3b8;
}

.info-value {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.status-selector {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.status-option {
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.status-option.selected {
  opacity: 1;
}

.status-option input {
  display: none;
}

.option-badge {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.status-option.selected .option-badge {
  border-color: currentColor;
}

.handle-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
}

.checkbox-item input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.note-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.note-section label {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.note-section textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

.note-section textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.panel-footer {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel {
  flex: 1;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: white;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

.btn-cancel:hover {
  background: #f8fafc;
}

.btn-submit {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #3b82f6;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-submit:hover:not(:disabled) {
  background: #2563eb;
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.confirm-dialog {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
}

.dialog-content {
  position: relative;
  background: white;
  border-radius: 12px;
  padding: 24px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.dialog-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.dialog-content p {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 24px 0;
}

.dialog-footer {
  display: flex;
  gap: 12px;
}

.dialog-footer .btn-cancel {
  flex: 1;
}

.dialog-footer .btn-confirm {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #3b82f6;
  color: white;
  font-size: 14px;
  cursor: pointer;
}

.dialog-footer .btn-confirm:hover {
  background: #2563eb;
}

.toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  z-index: 3000;
  animation: slideUp 0.3s ease;
}

.toast-success {
  background: #16a34a;
  color: white;
}

.toast-error {
  background: #dc2626;
  color: white;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes slideUp {
  from { transform: translateX(-50%) translateY(20px); opacity: 0; }
  to { transform: translateX(-50%) translateY(0); opacity: 1; }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }

  .slide-panel {
    width: 100%;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>