<template>
  <div class="panel">
    <h2 class="panel-title">商家工作台</h2>
    <div class="muted">商家注册、认证申请、预警消息与消费者反馈查看</div>
    <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>

    <section class="card">
      <h3>商家注册</h3>
      <div class="row">
        <input v-model="registerForm.username" placeholder="用户名" />
        <input v-model="registerForm.phone" placeholder="手机号" />
        <input v-model="registerForm.email" placeholder="邮箱" />
        <input v-model="registerForm.password" type="password" placeholder="密码" />
        <input v-model="registerForm.companyName" placeholder="企业名称" />
      </div>
      <div class="ops" style="margin-top:8px;"><button :disabled="loading" @click="onRegister">注册商家账号</button></div>
    </section>

    <section class="card">
      <h3>登录</h3>
      <div class="row">
        <input v-model="loginForm.account" placeholder="账号" />
        <input v-model="loginForm.password" type="password" placeholder="密码" />
      </div>
      <div class="ops" style="margin-top:8px;">
        <button :disabled="loading" @click="onLogin">登录</button>
        <button class="secondary" :disabled="loading" @click="syncToken">保存Token</button>
        <button class="danger" :disabled="loading" @click="onLogout">退出登录</button>
      </div>
      <textarea v-model="token" placeholder="Bearer Token（调试可粘贴）" style="margin-top:8px;"></textarea>
    </section>

    <section class="card">
      <h3>企业认证申请</h3>
      <div class="row">
        <input v-model="applyForm.companyId" placeholder="companyId" />
        <input v-model="applyForm.companyName" placeholder="企业名称" />
        <input v-model="applyForm.remark" placeholder="申请备注" />
      </div>
      <div class="ops" style="margin-top:8px;">
        <button :disabled="loading" @click="onApply">提交申请</button>
        <button class="secondary" :disabled="loading" @click="onQueryStatus">查询认证状态</button>
      </div>
      <div class="muted" style="margin-top:8px;">状态：{{ statusText }}</div>
    </section>

    <section class="card">
      <h3>消息系统</h3>
      <div class="ops" style="margin-bottom:8px;"><button :disabled="loading" @click="loadMessages">刷新消息</button></div>
      <table>
        <thead><tr><th>预警ID</th><th>二维码</th><th>等级</th><th>原因</th><th>动作结果</th></tr></thead>
        <tbody>
        <tr v-for="item in messages" :key="item.alertId">
          <td>{{ item.alertId }}</td><td>{{ item.qsId }}</td><td>{{ item.alertLevel }}</td><td>{{ item.reason }}</td><td>{{ item.actionResult }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>消费者反馈查看</h3>
      <div class="ops" style="margin-bottom:8px;"><button :disabled="loading" @click="loadFeedbacks">刷新反馈</button></div>
      <table>
        <thead><tr><th>反馈编号</th><th>二维码</th><th>类型</th><th>地区</th><th>风险</th><th>状态</th></tr></thead>
        <tbody>
        <tr v-for="item in feedbacks" :key="item.feedbackId">
          <td>{{ item.feedbackId }}</td><td>{{ item.qsId }}</td><td>{{ item.feedbackType }}</td><td>{{ item.region }}</td><td>{{ item.riskLevel }}</td><td>{{ item.status }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>调试输出</h3>
      <pre>{{ JSON.stringify(debugData, null, 2) }}</pre>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getMessages } from '../api/alert';
import { getFeedbackList } from '../api/feedback';
import { login, queryCompanyStatus, registerCompany, submitCompanyApply } from '../api/auth';
import { clearToken, getToken, setToken } from '../api/session';

const token = ref(getToken('company'));
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const registerForm = reactive({ username: '', phone: '', email: '', password: '', role: 'COMPANY', companyName: '' });
const loginForm = reactive({ account: '', password: '' });
const applyForm = reactive({ companyId: '', companyName: '', remark: '' });
const statusText = ref('未查询');
const messages = ref([]);
const feedbacks = ref([]);
const debugData = ref({});

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

function syncToken() {
  token.value = setToken('company', token.value);
  setNotice('商家Token已保存');
}

function onLogout() {
  clearToken('company');
  token.value = '';
  messages.value = [];
  feedbacks.value = [];
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
    setNotice(error?.message || '请求失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function onRegister() {
  await withLoading(async () => {
    const res = await registerCompany(registerForm);
    if (res.code === 200 && res.data) {
      applyForm.companyId = res.data.companyId || '';
      applyForm.companyName = res.data.companyName || registerForm.companyName;
    }
    debugData.value = res;
  }, '商家注册请求已提交');
}

async function onLogin() {
  await withLoading(async () => {
    const res = await login(loginForm.account, loginForm.password);
    if (res.code === 200 && res.data) {
      token.value = setToken('company', res.data);
      await Promise.all([loadMessages(), loadFeedbacks()]);
    }
    debugData.value = res;
  }, '商家登录成功');
}

async function onApply() {
  await withLoading(async () => {
    const res = await submitCompanyApply(applyForm, token.value);
    if (res.code === 200 && res.data) {
      statusText.value = res.data.statusText || 'PENDING';
    }
    debugData.value = res;
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
    debugData.value = res;
  });
}

async function loadMessages() {
  const res = await getMessages(token.value);
  messages.value = res.data || [];
  debugData.value = res;
}

async function loadFeedbacks() {
  const res = await getFeedbackList(token.value);
  feedbacks.value = res.data || [];
  debugData.value = res;
}

onMounted(async () => {
  if (!token.value) return;
  await withLoading(async () => {
    await Promise.all([loadMessages(), loadFeedbacks()]);
  });
});
</script>

