<template>
  <div class="auth-wrap">
    <section class="card auth-card">
      <h2 class="panel-title">商家登录</h2>
      <p class="muted">登录后可查看本企业消息、反馈与认证申请状态。</p>
      <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>
      <div class="row">
        <input v-model="form.account" placeholder="账号" />
        <input v-model="form.password" type="password" placeholder="密码" />
      </div>
      <div class="ops" style="margin-top: 10px;">
        <button :disabled="loading" @click="onLogin">登录</button>
        <RouterLink class="link-btn" to="/company/register">去注册</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { login } from '../api/auth';
import { setToken } from '../api/session';

const router = useRouter();
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const form = reactive({ account: '', password: '' });

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

async function onLogin() {
  loading.value = true;
  try {
    const res = await login(form.account, form.password);
    if (res?.data) {
      setToken('company', res.data);
      setNotice('登录成功，正在进入工作台...');
      router.push('/company/dashboard');
      return;
    }
    setNotice('登录失败，请检查账号密码', 'error');
  } catch (error) {
    setNotice(error?.message || '登录失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}
</script>


