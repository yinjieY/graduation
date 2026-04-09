<template>
  <div class="panel">
    <div class="toolbar">
      <div>
        <h2 class="panel-title">管理员工作台</h2>
        <p class="muted">企业审查、系统消息与消费者反馈处理</p>
      </div>
      <div class="ops">
        <button class="danger" :disabled="loading" @click="logout">退出登录</button>
      </div>
    </div>

    <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">
      {{ notice }}
    </div>

    <section class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3>企业审查</h3>
        <button :disabled="loading" @click="loadPending">刷新待审列表</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>备注</th><th>操作</th></tr></thead>
          <tbody>
          <tr v-if="pendingList.length === 0"><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 24px;">暂无待审企业</td></tr>
          <tr v-for="item in pendingList" :key="item.companyId">
            <td>{{ item.companyId }}</td>
            <td style="font-weight: 500;">{{ item.companyName }}</td>
            <td>{{ item.applyBy }}</td>
            <td style="max-width: 200px; white-space: normal;">{{ item.remark }}</td>
            <td class="ops">
              <button :disabled="loading" @click="review(item.companyId, true)">通过</button>
              <button class="secondary" :disabled="loading" @click="review(item.companyId, false)">拒绝</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3>系统消息</h3>
        <button :disabled="loading" @click="loadMessages">刷新消息</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead><tr><th>预警ID</th><th>企业</th><th>等级</th><th>原因</th><th>状态</th><th>动作结果</th></tr></thead>
          <tbody>
          <tr v-if="filteredMessages.length === 0"><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 24px;">暂无系统消息</td></tr>
          <tr v-for="item in filteredMessages" :key="item.alertId" :class="{ highlight: query.alertId && String(item.alertId) === String(query.alertId) }">
            <td>{{ item.alertId }}</td>
            <td>{{ item.companyId }}</td>
            <td>
              <span :style="{ color: item.alertLevel === 'HIGH' ? 'var(--danger)' : 'var(--primary)' }">
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
    </section>

    <section class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
        <h3>反馈处理</h3>
        <button :disabled="loading" @click="loadFeedbacks">刷新反馈</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead><tr><th>反馈编号</th><th>二维码</th><th>企业</th><th>类型</th><th>风险</th><th>状态</th><th>提交IP</th><th>操作</th></tr></thead>
          <tbody>
          <tr v-if="filteredFeedbacks.length === 0"><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 24px;">暂无反馈数据</td></tr>
          <tr v-for="item in filteredFeedbacks" :key="item.feedbackId" :class="{ highlight: query.feedbackId && String(item.feedbackId) === String(query.feedbackId) }">
            <td>{{ item.feedbackId }}</td>
            <td>{{ item.qsId }}</td>
            <td>{{ item.companyId }}</td>
            <td>{{ item.feedbackType }}</td>
            <td>{{ item.riskLevel }}</td>
            <td>
              <span style="padding: 4px 8px; border-radius: 4px; background: var(--panel-soft); font-size: 12px; border: 1px solid var(--border);">
                {{ item.status }}
              </span>
            </td>
            <td>{{ item.submitterIpMasked || '-' }}</td>
            <td class="ops">
              <button :disabled="loading" @click="loadDetail(item.feedbackId)">详情</button>
              <button :disabled="loading" @click="updateStatus(item.feedbackId, 'ACCEPTED')">受理</button>
              <button class="secondary" :disabled="loading" @click="updateStatus(item.feedbackId, 'REJECTED')">驳回</button>
              <button class="danger" :disabled="loading" @click="updateStatus(item.feedbackId, 'CLOSED')">结案</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div class="detail-section" style="margin-top: 32px; padding-top: 24px; border-top: 1px dashed var(--border);">
        <h3 style="margin-bottom: 16px;">反馈详情与处置</h3>
        <div class="row" style="align-items: center; margin-bottom: 16px;">
          <input v-model="detailForm.feedbackId" placeholder="请输入反馈编号查阅" />
          <button :disabled="loading" @click="loadDetail(detailForm.feedbackId)" style="width: auto;">加载详情</button>
        </div>
        <textarea v-model="detailForm.handleNote" placeholder="处理备注（结案时将附带 submitterIp + 当前二维码状态）" style="margin-bottom: 16px;"></textarea>
        <pre v-if="Object.keys(detailData).length > 0">{{ JSON.stringify(detailData, null, 2) }}</pre>
        <div v-else style="padding: 20px; text-align: center; color: var(--text-muted); background: var(--panel-soft); border-radius: 8px; border: 1px solid var(--border);">
          请选择或输入反馈编号加载详情数据
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getMessages } from '../api/alert';
import { getFeedbackDetail, getFeedbackList, updateFeedbackStatus } from '../api/feedback';
import { getPending, reviewCompany } from '../api/auth';
import { clearToken, getToken } from '../api/session';

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

const query = computed(() => ({
  alertId: route.query.alertId || route.query.eventId || '',
  qsId: route.query.qsId || '',
  companyId: route.query.companyId || '',
  feedbackId: route.query.feedbackId || ''
}));

const filteredMessages = computed(() => messageList.value.filter((item) => {
  if (query.value.alertId && String(item.alertId) !== String(query.value.alertId)) return false;
  if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
  return true;
}));

const filteredFeedbacks = computed(() => feedbackList.value.filter((item) => {
  if (query.value.qsId && String(item.qsId) !== String(query.value.qsId)) return false;
  if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
  if (query.value.feedbackId && String(item.feedbackId) !== String(query.value.feedbackId)) return false;
  return true;
}));

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

function logout() {
  clearToken('admin');
  router.push('/admin/login');
}

async function withLoading(task, successMessage = '') {
  loading.value = true;
  try {
    await task();
    if (successMessage) setNotice(successMessage);
  } catch (error) {
    if (error?.status === 401) {
      logout();
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

onMounted(async () => {
  await withLoading(async () => {
    await Promise.all([loadPending(), loadMessages(), loadFeedbacks()]);
    if (query.value.feedbackId) {
      await loadDetail(String(query.value.feedbackId));
    }
  });
});
</script>