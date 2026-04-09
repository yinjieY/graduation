<template>
  <div class="auth-wrap">
    <div class="auth-container">
      <section class="card auth-card">
        <div class="auth-header">
          <h2 class="panel-title">管理员登录</h2>
          <p class="muted">登录后进入监管后台，查看预警消息与反馈处置。</p>
        </div>

        <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error' }">
          {{ notice }}
        </div>

        <div class="auth-form">
          <div class="form-group">
            <input v-model="form.account" placeholder="请输入管理员账号" />
          </div>
          <div class="form-group">
            <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="onLogin" />
          </div>
        </div>

        <div class="auth-actions">
          <button class="btn-block" :disabled="loading" @click="onLogin">
            {{ loading ? '登录中...' : '立即登录' }}
          </button>
          <div class="auth-links">
            <span class="muted">还没有账号？</span>
            <RouterLink class="text-link" to="/admin/register">前往注册</RouterLink>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import { login } from '../api/auth';
import { setToken } from '../api/session';

const router = useRouter();
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const form = reactive({ account: 'admin001', password: 'password' });

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
}

async function onLogin() {
  loading.value = true;
  try {
    const res = await login(form.account, form.password);
    if (res?.data) {
      setToken('admin', res.data);
      setNotice('登录成功，正在进入后台...');
      router.push('/admin/dashboard');
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

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  padding: 20px;
}

.auth-container {
  width: 100%;
  max-width: 440px;
}

.auth-card {
  padding: 40px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header .panel-title {
  font-size: 26px;
  color: var(--primary-hover);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.auth-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.btn-block {
  width: 100%;
  padding: 12px;
  font-size: 16px;
}

.auth-links {
  text-align: center;
  font-size: 14px;
}

.text-link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.text-link:hover {
  text-decoration: underline;
}
</style>