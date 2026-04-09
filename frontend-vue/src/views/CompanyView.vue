<template>
  <div class="panel">
    <div class="toolbar">
      <div>
        <h2 class="panel-title">商家工作台</h2>
        <div class="muted">企业认证申请、系统消息与反馈查看</div>
      </div>
      <div class="ops">
        <button class="danger" :disabled="loading" @click="logout">退出登录</button>
      </div>
    </div>

    <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>

    <section class="card">
      <h3>企业认证申请</h3>
      <div class="row">
        <input v-model="applyForm.companyId" placeholder="companyId" />
        <input v-model="applyForm.companyName" placeholder="企业名称" />
        <input v-model="applyForm.remark" placeholder="申请备注" />
      </div>
      <div class="ops" style="margin-top: 8px;">
        <button :disabled="loading" @click="onApply">提交申请</button>
        <button class="secondary" :disabled="loading" @click="onQueryStatus">查询认证状态</button>
      </div>
      <div class="muted" style="margin-top: 8px;">状态：{{ statusText }}</div>
    </section>

    <section class="card">
      <h3>系统消息</h3>
      <div class="ops" style="margin-bottom: 8px;"><button :disabled="loading" @click="loadMessages">刷新消息</button></div>
      <table>
        <thead><tr><th>预警ID</th><th>二维码</th><th>等级</th><th>原因</th><th>动作结果</th><th>状态</th></tr></thead>
        <tbody>
        <tr v-for="item in messages" :key="item.alertId">
          <td>{{ item.alertId }}</td>
          <td>{{ item.qsId }}</td>
          <td>{{ item.alertLevel }}</td>
          <td>{{ item.reason }}</td>
          <td>{{ item.actionResult }}</td>
          <td>{{ item.status }}</td>
        </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h3>消费者反馈查看</h3>
      <div class="ops" style="margin-bottom: 8px;"><button :disabled="loading" @click="loadFeedbacks">刷新反馈</button></div>
      <table>
        <thead><tr><th>反馈编号</th><th>二维码</th><th>类型</th><th>地区</th><th>风险</th><th>状态</th><th>提交IP</th></tr></thead>
        <tbody>
        <tr v-for="item in feedbacks" :key="item.feedbackId">
          <td>{{ item.feedbackId }}</td>
          <td>{{ item.qsId }}</td>
          <td>{{ item.feedbackType }}</td>
          <td>{{ item.region }}</td>
          <td>{{ item.riskLevel }}</td>
          <td>{{ item.status }}</td>
          <td>{{ item.submitterIpMasked || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getMessages } from '../api/alert';
import { getFeedbackList } from '../api/feedback';
import { queryCompanyStatus, submitCompanyApply } from '../api/auth';
import { clearToken, getToken } from '../api/session';
import { useRouter } from 'vue-router';

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
}

function logout() {
  clearToken('company');
  router.push('/company/login');
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

