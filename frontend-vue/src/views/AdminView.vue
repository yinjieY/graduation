<template>
  <div class="panel">
    <h2 class="panel-title">管理员工作台</h2>
    <div class="muted">企业审查、预警消息与消费者反馈处置</div>
    <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>

    <section class="card">
      <h3>登录</h3>
      <div class="row">
        <input v-model="loginForm.account" placeholder="账号" />
        <input v-model="loginForm.password" placeholder="密码" type="password" />
      </div>
      <div class="ops" style="margin-top:8px;">
        <button :disabled="loading" @click="onLogin">登录并加载数据</button>
        <button class="secondary" :disabled="loading" @click="syncToken">保存Token</button>
        <button class="danger" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
      <textarea v-model="token" placeholder="Bearer Token（调试可直接粘贴）" style="margin-top:8px;"></textarea>
    </section>

    <section class="card">
      <h3>管理员注册</h3>
      <div class="row">
        <input v-model="registerForm.username" placeholder="用户名" />
        <input v-model="registerForm.phone" placeholder="手机号" />
        <input v-model="registerForm.email" placeholder="邮箱" />
        <input v-model="registerForm.password" placeholder="密码" type="password" />
      </div>
      <div class="ops" style="margin-top:8px;"><button :disabled="loading" @click="onRegister">注册</button></div>
    </section>

    <section class="card">
      <h3>企业审查</h3>
      <div class="ops" style="margin-bottom:8px;"><button :disabled="loading" @click="loadPending">刷新待审列表</button></div>
      <table>
        <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>
        <tr v-for="item in pendingList" :key="item.companyId">
          <td>{{ item.companyId }}</td>
          <td>{{ item.companyName }}</td>
          <td>{{ item.applyBy }}</td>
          <td>{{ item.remark }}</td>
          <td class="ops">
            <button :disabled="loading" @click="review(item.companyId, true)">通过</button>
            <button class="secondary" :disabled="loading" @click="review(item.companyId, false)">拒绝</button>
          </td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>消息系统</h3>
      <div class="ops" style="margin-bottom:8px;"><button :disabled="loading" @click="loadMessages">刷新消息</button></div>
      <table>
        <thead><tr><th>预警ID</th><th>企业</th><th>等级</th><th>原因</th><th>状态</th><th>动作结果</th></tr></thead>
        <tbody>
        <tr v-for="item in filteredMessages" :key="item.alertId" :class="{ highlight: query.alertId && String(item.alertId) === String(query.alertId) }">
          <td>{{ item.alertId }}</td>
          <td>{{ item.companyId }}</td>
          <td>{{ item.alertLevel }}</td>
          <td>{{ item.reason }}</td>
          <td>{{ item.status }}</td>
          <td>{{ item.actionResult }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>反馈处理</h3>
      <div class="ops" style="margin-bottom:8px;"><button :disabled="loading" @click="loadFeedbacks">刷新反馈</button></div>
      <table>
        <thead><tr><th>反馈编号</th><th>二维码</th><th>企业</th><th>类型</th><th>风险</th><th>状态</th><th>提交IP</th><th>操作</th></tr></thead>
        <tbody>
        <tr v-for="item in filteredFeedbacks" :key="item.feedbackId" :class="{ highlight: query.feedbackId && String(item.feedbackId) === String(query.feedbackId) }">
          <td>{{ item.feedbackId }}</td>
          <td>{{ item.qsId }}</td>
          <td>{{ item.companyId }}</td>
          <td>{{ item.feedbackType }}</td>
          <td>{{ item.riskLevel }}</td>
          <td>{{ item.status }}</td>
          <td>{{ item.submitterIpMasked }}</td>
          <td class="ops">
            <button :disabled="loading" @click="loadDetail(item.feedbackId)">详情</button>
            <button :disabled="loading" @click="updateStatus(item.feedbackId, 'ACCEPTED')">受理</button>
            <button class="secondary" :disabled="loading" @click="updateStatus(item.feedbackId, 'REJECTED')">驳回</button>
          </td>
        </tr>
        </tbody>
      </table>

      <div class="card" style="margin-top:12px;">
        <h3>反馈详情</h3>
        <div class="row">
          <input v-model="detailForm.feedbackId" placeholder="反馈编号" />
          <button :disabled="loading" @click="loadDetail(detailForm.feedbackId)">加载详情</button>
        </div>
        <textarea v-model="detailForm.handleNote" placeholder="处理备注（会记录在结案说明中）" style="margin-top:8px;"></textarea>
        <div class="ops" style="margin-top:8px;">
          <button :disabled="loading" @click="updateStatus(detailForm.feedbackId, 'ACCEPTED')">受理</button>
          <button class="secondary" :disabled="loading" @click="updateStatus(detailForm.feedbackId, 'REJECTED')">驳回</button>
          <button :disabled="loading" @click="updateStatus(detailForm.feedbackId, 'CLOSED')">结案</button>
        </div>
        <pre style="margin-top:8px;">{{ JSON.stringify(detailData, null, 2) }}</pre>
      </div>
    </section>

    <section class="card">
      <h3>调试输出</h3>
      <pre>{{ JSON.stringify(debugData, null, 2) }}</pre>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { getMessages } from '../api/alert';
import { getFeedbackDetail, getFeedbackList, updateFeedbackStatus } from '../api/feedback';
import { getPending, login, registerAdmin, reviewCompany } from '../api/auth';
import { clearToken, getToken, setToken } from '../api/session';

const route = useRoute();
const token = ref(getToken('admin'));
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const loginForm = reactive({ account: 'admin001', password: 'password' });
const registerForm = reactive({ username: '', phone: '', email: '', password: '' });
const detailForm = reactive({ feedbackId: '', handleNote: '' });
const pendingList = ref([]);
const messageList = ref([]);
const feedbackList = ref([]);
const detailData = ref({});
const debugData = ref({});

const query = computed(() => ({
  alertId: route.query.alertId || route.query.eventId || '',
  qsId: route.query.qsId || '',
  companyId: route.query.companyId || '',
  feedbackId: route.query.feedbackId || ''
}));

const filteredMessages = computed(() => {
  return messageList.value.filter((item) => {
    if (query.value.alertId && String(item.alertId) !== String(query.value.alertId)) return false;
    if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
    return true;
  });
});

const filteredFeedbacks = computed(() => {
  return feedbackList.value.filter((item) => {
    if (query.value.qsId && String(item.qsId) !== String(query.value.qsId)) return false;
    if (query.value.companyId && String(item.companyId) !== String(query.value.companyId)) return false;
    if (query.value.feedbackId && String(item.feedbackId) !== String(query.value.feedbackId)) return false;
    return true;
  });
});

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

function resolveErrorMessage(error) {
  return error?.message || '请求失败，请稍后再试';
}

function syncToken() {
  token.value = setToken('admin', token.value);
  setNotice('管理员Token已保存');
}

function onLogout() {
  clearToken('admin');
  token.value = '';
  pendingList.value = [];
  messageList.value = [];
  feedbackList.value = [];
  detailData.value = {};
  setNotice('已退出登录');
}

async function withLoading(task, successMessage = '') {
  loading.value = true;
  try {
    await task();
    if (successMessage) {
      setNotice(successMessage);
    }
  } catch (error) {
    if (error?.status === 401) {
      onLogout();
      setNotice('登录已过期，请重新登录', 'error');
      return;
    }
    setNotice(resolveErrorMessage(error), 'error');
  } finally {
    loading.value = false;
  }
}

async function onLogin() {
  await withLoading(async () => {
    const res = await login(loginForm.account, loginForm.password);
    if (res.code === 200 && res.data) {
      token.value = setToken('admin', res.data);
      await Promise.all([loadPending(), loadMessages(), loadFeedbacks()]);
      if (query.value.feedbackId) {
        await loadDetail(String(query.value.feedbackId));
      }
    }
    debugData.value = res;
  }, '管理员登录成功');
}

async function onRegister() {
  await withLoading(async () => {
    const res = await registerAdmin({ ...registerForm, role: 'ADMIN' });
    debugData.value = res;
  }, '管理员注册请求已提交');
}

async function loadPending() {
  const res = await getPending(token.value);
  pendingList.value = res.data || [];
  debugData.value = res;
}

async function review(companyId, approved) {
  await withLoading(async () => {
    const res = await reviewCompany(companyId, approved, token.value);
    debugData.value = res;
    await loadPending();
  }, approved ? '已通过企业申请' : '已拒绝企业申请');
}

async function loadMessages() {
  const res = await getMessages(token.value);
  messageList.value = res.data || [];
  debugData.value = res;
}

async function loadFeedbacks() {
  const res = await getFeedbackList(token.value);
  feedbackList.value = res.data || [];
  debugData.value = res;
}

async function loadDetail(feedbackId) {
  if (!feedbackId) return;
  const res = await getFeedbackDetail(feedbackId, token.value);
  detailData.value = res.data || {};
  detailForm.feedbackId = feedbackId;
  debugData.value = res;
}

async function updateStatus(feedbackId, status) {
  if (!feedbackId) return;
  await withLoading(async () => {
    const res = await updateFeedbackStatus(feedbackId, status, detailForm.handleNote, token.value);
    debugData.value = res;
    await loadFeedbacks();
    await loadDetail(feedbackId);
  }, `反馈 ${feedbackId} 已更新为 ${status}`);
}

onMounted(async () => {
  if (!token.value) return;
  await withLoading(async () => {
    await Promise.all([loadMessages(), loadFeedbacks(), loadPending()]);
    if (query.value.feedbackId) {
      await loadDetail(String(query.value.feedbackId));
    }
  });
});
</script>

