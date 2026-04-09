<template>
  <div class="auth-wrap">
    <div class="auth-container">
      <section class="card auth-card">
        <div class="auth-header">
          <h2 class="panel-title">管理员注册</h2>
          <p class="muted">用于毕设演示环境，生产环境请限制管理员注册入口。</p>
        </div>

        <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error', success: noticeType === 'success' }">
          {{ notice }}
        </div>

        <form class="auth-form" @submit.prevent="onRegister">
          <div class="form-group">
            <label for="username">用户名</label>
            <input 
              id="username"
              v-model="form.username" 
              placeholder="请输入用户名" 
              @blur="validateField('username')"
              :class="{ 'invalid': errors.username }"
            />
            <div v-if="errors.username" class="error-message">{{ errors.username }}</div>
          </div>
          <div class="form-group">
            <label for="phone">手机号</label>
            <input 
              id="phone"
              v-model="form.phone" 
              placeholder="请输入手机号" 
              @blur="validateField('phone')"
              :class="{ 'invalid': errors.phone }"
            />
            <div v-if="errors.phone" class="error-message">{{ errors.phone }}</div>
          </div>
          <div class="form-group">
            <label for="email">邮箱</label>
            <input 
              id="email"
              v-model="form.email" 
              placeholder="请输入邮箱" 
              @blur="validateField('email')"
              :class="{ 'invalid': errors.email }"
            />
            <div v-if="errors.email" class="error-message">{{ errors.email }}</div>
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input 
              id="password"
              v-model="form.password" 
              type="password" 
              placeholder="请输入密码" 
              @blur="validateField('password')"
              :class="{ 'invalid': errors.password }"
            />
            <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
          </div>
        </form>

        <div class="auth-actions">
          <button class="btn-block" :disabled="loading" @click="onRegister">
            <span v-if="loading" class="loading-spinner"></span>
            {{ loading ? '注册中...' : '注册' }}
          </button>
          <div class="auth-links">
            <span class="muted">已有账号？</span>
            <RouterLink class="text-link" to="/admin/login">返回登录</RouterLink>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import { registerAdmin } from '../api/auth';

const router = useRouter();
const loading = ref(false);
const notice = ref('');
const noticeType = ref('info');
const form = reactive({ username: '', phone: '', email: '', password: '', role: 'ADMIN' });
const errors = reactive({ username: '', phone: '', email: '', password: '' });

function setNotice(message, type = 'info') {
  notice.value = message;
  noticeType.value = type;
  if (message) {
    setTimeout(() => {
      notice.value = '';
    }, 3000);
  }
}

function validateField(field) {
  switch (field) {
    case 'username':
      if (!form.username.trim()) {
        errors.username = '请输入用户名';
      } else if (form.username.length < 3) {
        errors.username = '用户名长度至少3位';
      } else {
        errors.username = '';
      }
      break;
    case 'phone':
      if (!form.phone) {
        errors.phone = '请输入手机号';
      } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
        errors.phone = '请输入正确的手机号';
      } else {
        errors.phone = '';
      }
      break;
    case 'email':
      if (!form.email) {
        errors.email = '请输入邮箱';
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
        errors.email = '请输入正确的邮箱地址';
      } else {
        errors.email = '';
      }
      break;
    case 'password':
      if (!form.password) {
        errors.password = '请输入密码';
      } else if (form.password.length < 6) {
        errors.password = '密码长度至少6位';
      } else {
        errors.password = '';
      }
      break;
  }
}

function validateForm() {
  let isValid = true;
  Object.keys(form).forEach(field => {
    if (field !== 'role') {
      validateField(field);
      if (errors[field]) {
        isValid = false;
      }
    }
  });
  return isValid;
}

async function onRegister() {
  if (!validateForm()) {
    setNotice('请检查输入信息', 'error');
    return;
  }
  
  loading.value = true;
  try {
    await registerAdmin(form);
    setNotice('注册成功，请登录', 'success');
    setTimeout(() => router.push('/admin/login'), 1000);
  } catch (error) {
    setNotice(error?.message || '注册失败，请稍后重试', 'error');
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
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  background: #fff;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header .panel-title {
  font-size: 26px;
  color: var(--primary-hover);
  margin-bottom: 8px;
  font-weight: 700;
}

.auth-header .muted {
  color: #6b7280;
  font-size: 14px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-group input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-group input.invalid {
  border-color: #ef4444;
}

.error-message {
  font-size: 12px;
  color: #ef4444;
  margin-top: 2px;
}

.status-line {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.status-line.error {
  background-color: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.status-line.success {
  background-color: #d1fae5;
  color: #065f46;
  border: 1px solid #bbf7d0;
}

.auth-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.btn-block {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-block:hover:not(:disabled) {
  background: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-block:disabled {
  background: #93c5fd;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
  transition: color 0.2s ease;
}

.text-link:hover {
  text-decoration: underline;
  color: var(--primary-hover);
}

@media (max-width: 480px) {
  .auth-card {
    padding: 30px 20px;
  }
  
  .auth-header .panel-title {
    font-size: 22px;
  }
  
  .form-group input {
    padding: 10px 14px;
  }
  
  .btn-block {
    padding: 12px;
  }
}
</style>

