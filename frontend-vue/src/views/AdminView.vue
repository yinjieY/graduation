<template>
  <div>
    <h2>管理员页面</h2>
    <div class="muted">Vue3版本：企业审查 + 消息 + 反馈处理</div>

    <section class="card">
      <h3>登录</h3>
      <div class="row">
        <input v-model="loginForm.account" placeholder="账号" />
        <input v-model="loginForm.password" placeholder="密码" type="password" />
        <button @click="onLogin">登录</button>
      </div>
      <textarea v-model="token" placeholder="Bearer Token" style="margin-top:8px;"></textarea>
    </section>

    <section class="card">
      <h3>管理员注册</h3>
      <div class="row">
        <input v-model="registerForm.username" placeholder="用户名" />
        <input v-model="registerForm.phone" placeholder="手机号" />
        <input v-model="registerForm.email" placeholder="邮箱" />
        <input v-model="registerForm.password" placeholder="密码" type="password" />
      </div>
      <div style="margin-top:8px;"><button @click="onRegister">注册</button></div>
    </section>

    <section class="card">
      <h3>企业审查</h3>
      <div class="ops" style="margin-bottom:8px;"><button @click="loadPending">刷新待审列表</button></div>
      <table>
        <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>备注</th><th>操作</th></tr></thead>
        <tbody>
        <tr v-for="item in pendingList" :key="item.companyId">
          <td>{{ item.companyId }}</td>
          <td>{{ item.companyName }}</td>
          <td>{{ item.applyBy }}</td>
          <td>{{ item.remark }}</td>
          <td class="ops">
            <button @click="review(item.companyId, true)">通过</button>
            <button class="secondary" @click="review(item.companyId, false)">拒绝</button>
          </td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>消息系统</h3>
      <div class="ops" style="margin-bottom:8px;"><button @click="loadMessages">刷新消息</button></div>
      <table>
        <thead><tr><th>预警ID</th><th>企业</th><th>等级</th><th>原因</th><th>状态</th><th>动作结果</th></tr></thead>
        <tbody>
        <tr v-for="item in filteredMessages" :key="item.alertId" :class="{ highlight: query.alertId && String(item.alertId) === query.alertId }">
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
      <div class="ops" style="margin-bottom:8px;"><button @click="loadFeedbacks">刷新反馈</button></div>
      <table>
        <thead><tr><th>反馈编号</th><th>二维码</th><th>企业</th><th>类型</th><th>风险</th><th>状态</th><th>提交IP</th><th>操作</th></tr></thead>
        <tbody>
        <tr
          v-for="item in filteredFeedbacks"
          :key="item.feedbackId"
          :class="{ highlight: query.feedbackId && item.feedbackId === query.feedbackId }"
        >
          <td>{{ item.feedbackId }}</td>
          <td>{{ item.qsId }}</td>
          <td>{{ item.companyId }}</td>
          <td>{{ item.feedbackType }}</td>
          <td>{{ item.riskLevel }}</td>
          <td>{{ item.status }}</td>
          <td>{{ item.submitterIpMasked }}</td>
          <td class="ops">
            <button @click="loadDetail(item.feedbackId)">详情</button>
            <button @click="updateStatus(item.feedbackId, 'ACCEPTED')">受理</button>
            <button class="secondary" @click="updateStatus(item.feedbackId, 'REJECTED')">驳回</button>
          </td>
        </tr>
        </tbody>
      </table>

      <div class="card" style="margin-top:12px;">
        <h3>反馈详情</h3>
        <div class="row">
          <input v-model="detailForm.feedbackId" placeholder="反馈编号" />
          <input v-model="detailForm.status" placeholder="状态(ACCEPTED/REJECTED/CLOSED)" />
          <button @click="loadDetail(detailForm.feedbackId)">加载详情</button>
        </div>
        <textarea v-model="detailForm.handleNote" placeholder="处理备注" style="margin-top:8px;"></textarea>
        <div class="ops" style="margin-top:8px;">
          <button @click="updateStatus(detailForm.feedbackId, 'ACCEPTED')">受理</button>
          <button class="secondary" @click="updateStatus(detailForm.feedbackId, 'REJECTED')">驳回</button>
          <button @click="updateStatus(detailForm.feedbackId, 'CLOSED')">结案</button>
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

const route = useRoute();
const token = ref(localStorage.getItem('adminToken') || '');
const loginForm = reactive({ account: 'admin001', password: 'password' });
const registerForm = reactive({ username: '', phone: '', email: '', password: '' });
const detailForm = reactive({ feedbackId: '', status: '', handleNote: '' });
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

async function onLogin() {
  const res = await login(loginForm.account, loginForm.password);
  if (res.code === 200 && res.data) {
    token.value = `Bearer ${res.data}`;
    localStorage.setItem('adminToken', token.value);
    await loadPending();
    await loadMessages();
    await loadFeedbacks();
    if (query.value.feedbackId) {
      await loadDetail(String(query.value.feedbackId));
    }
  }
  debugData.value = res;
}

async function onRegister() {
  const res = await registerAdmin({ ...registerForm, role: 'ADMIN' });
  debugData.value = res;
}

async function loadPending() {
  const res = await getPending(token.value);
  pendingList.value = res.data || [];
  debugData.value = res;
}

async function review(companyId, approved) {
  const res = await reviewCompany(companyId, approved, token.value);
  debugData.value = res;
  await loadPending();
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
  detailForm.status = detailData.value.feedbackStatus || '';
  debugData.value = res;
}

async function updateStatus(feedbackId, status) {
  if (!feedbackId) return;
  const res = await updateFeedbackStatus(feedbackId, status, detailForm.handleNote, token.value);
  debugData.value = res;
  await loadFeedbacks();
  await loadDetail(feedbackId);
}

onMounted(async () => {
  if (token.value) {
    await loadMessages();
    await loadFeedbacks();
  }
});
</script>

