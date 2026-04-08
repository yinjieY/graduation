<template>
  <div>
    <h2>商家页面</h2>
    <div class="muted">Vue3版本：商家注册、认证申请、消息与反馈</div>

    <section class="card">
      <h3>商家注册</h3>
      <div class="row">
        <input v-model="registerForm.username" placeholder="用户名" />
        <input v-model="registerForm.phone" placeholder="手机号" />
        <input v-model="registerForm.email" placeholder="邮箱" />
        <input v-model="registerForm.password" type="password" placeholder="密码" />
        <input v-model="registerForm.companyName" placeholder="企业名称" />
      </div>
      <div style="margin-top:8px;"><button @click="onRegister">注册商家账号</button></div>
    </section>

    <section class="card">
      <h3>登录</h3>
      <div class="row">
        <input v-model="loginForm.account" placeholder="账号" />
        <input v-model="loginForm.password" type="password" placeholder="密码" />
        <button @click="onLogin">登录</button>
      </div>
      <textarea v-model="token" placeholder="Bearer Token" style="margin-top:8px;"></textarea>
    </section>

    <section class="card">
      <h3>企业认证申请</h3>
      <div class="row">
        <input v-model="applyForm.companyId" placeholder="companyId" />
        <input v-model="applyForm.companyName" placeholder="企业名称" />
        <input v-model="applyForm.remark" placeholder="申请备注" />
      </div>
      <div class="ops" style="margin-top:8px;">
        <button @click="onApply">提交申请</button>
        <button @click="onQueryStatus">查询认证状态</button>
      </div>
      <div class="muted" style="margin-top:8px;">状态：{{ statusText }}</div>
    </section>

    <section class="card">
      <h3>消息系统</h3>
      <div class="ops" style="margin-bottom:8px;"><button @click="loadMessages">刷新消息</button></div>
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
      <div class="ops" style="margin-bottom:8px;"><button @click="loadFeedbacks">刷新反馈</button></div>
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
import { reactive, ref } from 'vue';
import { getMessages } from '../api/alert';
import { getFeedbackList } from '../api/feedback';
import { login, queryCompanyStatus, registerCompany, submitCompanyApply } from '../api/auth';

const token = ref(localStorage.getItem('companyToken') || '');
const registerForm = reactive({ username: '', phone: '', email: '', password: '', role: 'COMPANY', companyName: '' });
const loginForm = reactive({ account: '', password: '' });
const applyForm = reactive({ companyId: '', companyName: '', remark: '' });
const statusText = ref('未查询');
const messages = ref([]);
const feedbacks = ref([]);
const debugData = ref({});

async function onRegister() {
  const res = await registerCompany(registerForm);
  if (res.code === 200 && res.data) {
    applyForm.companyId = res.data.companyId || '';
    applyForm.companyName = res.data.companyName || registerForm.companyName;
  }
  debugData.value = res;
}

async function onLogin() {
  const res = await login(loginForm.account, loginForm.password);
  if (res.code === 200 && res.data) {
    token.value = `Bearer ${res.data}`;
    localStorage.setItem('companyToken', token.value);
  }
  debugData.value = res;
}

async function onApply() {
  const res = await submitCompanyApply(applyForm, token.value);
  if (res.code === 200 && res.data) {
    statusText.value = res.data.statusText || 'PENDING';
  }
  debugData.value = res;
}

async function onQueryStatus() {
  if (!applyForm.companyId) return;
  const res = await queryCompanyStatus(applyForm.companyId, token.value);
  if (res.code === 200 && res.data) {
    statusText.value = `${res.data.statusText || ''} ${res.data.remark || ''}`.trim();
  }
  debugData.value = res;
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
</script>

