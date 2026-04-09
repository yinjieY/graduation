cd<template>
  <div class="auth-wrap">
    <section class="card auth-card">
      <h2 class="panel-title">商家注册</h2>
      <p class="muted">注册后可登录并发起企业资质认证申请。</p>
      <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">{{ notice }}</div>
      <div class="row">
        <input v-model="form.username" placeholder="用户名" />
        <input v-model="form.phone" placeholder="手机号" />
        <input v-model="form.email" placeholder="邮箱" />
        <input v-model="form.password" type="password" placeholder="密码" />
        <input v-model="form.companyName" placeholder="企业名称" />
      </div>
      <div class="ops" style="margin-top: 10px;">
        <button :disabled="loading" @click="onRegister">注册</button>
        <RouterLink class="link-btn" to="/company/login">返回登录</RouterLink>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { registerCompany } from '../api/auth';

const router = useRouter();
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const form = reactive({ username: '', phone: '', email: '', password: '', role: 'COMPANY', companyName: '' });

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

async function onRegister() {
  loading.value = true;
  try {
    await registerCompany(form);
    setNotice('注册成功，请登录');
    setTimeout(() => router.push('/company/login'), 500);
  } catch (error) {
    setNotice(error?.message || '注册失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}
</script>

