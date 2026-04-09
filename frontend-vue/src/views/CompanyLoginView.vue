<template>
  <div class="auth-wrap company-login">
    <div class="auth-bg"></div>
    <div class="auth-container">
      <section class="card auth-card">
        <div class="auth-header">
          <div class="header-logo">
            <div class="logo-icon">🏭</div>
            <div class="logo-text">商家中心</div>
          </div>
          <h2 class="panel-title">商家登录</h2>
          <p class="muted">登录后可查看本企业消息、反馈与认证申请状态。</p>
        </div>

        <div v-if="notice" class="status-line" :class="{ error: noticeType === 'error', success: noticeType === 'success' }">
          <span class="status-icon" :class="noticeType">{{ noticeType === 'success' ? '✓' : '✕' }}</span>
          {{ notice }}
        </div>

        <form class="auth-form" @submit.prevent="onLogin">
          <div class="form-group">
            <label for="account">账号</label>
            <div class="input-wrapper">
              <span class="input-icon">👤</span>
              <input 
                id="account"
                v-model="form.account" 
                placeholder="请输入账号" 
                @blur="validateField('account')"
                :class="{ 'invalid': errors.account }"
              />
            </div>
            <div v-if="errors.account" class="error-message">{{ errors.account }}</div>
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input 
                id="password"
                v-model="form.password" 
                type="password" 
                placeholder="请输入密码" 
                @keyup.enter="onLogin"
                @blur="validateField('password')"
                :class="{ 'invalid': errors.password }"
              />
            </div>
            <div v-if="errors.password" class="error-message">{{ errors.password }}</div>
          </div>
        </form>

        <div class="auth-actions">
          <button class="btn-block" :disabled="loading" @click="onLogin">
            <span v-if="loading" class="loading-spinner"></span>
            {{ loading ? '登录中...' : '登录' }}
          </button>
          <div class="auth-links">
            <span class="muted">还没有账号？</span>
            <RouterLink class="text-link" to="/company/register">去注册</RouterLink>
          </div>
        </div>

        <div class="auth-footer">
          <div class="footer-info">
            <span class="footer-item">
              <span class="footer-icon">📊</span>
              数据管理
            </span>
            <span class="footer-divider">|</span>
            <span class="footer-item">
              <span class="footer-icon">📱</span>
              消息通知
            </span>
            <span class="footer-divider">|</span>
            <span class="footer-item">
              <span class="footer-icon">✓</span>
              认证管理
            </span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import { useAuth } from '../composables/useAuth';

const router = useRouter();
const { handleLogin, loading } = useAuth();
const notice = ref('');
const noticeType = ref('info');
const form = reactive({ account: '', password: '' });
const errors = reactive({ account: '', password: '' });

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
    case 'account':
      if (!form.account.trim()) {
        errors.account = '请输入账号';
      } else {
        errors.account = '';
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
    validateField(field);
    if (errors[field]) {
      isValid = false;
    }
  });
  return isValid;
}

async function onLogin() {
  if (!validateForm()) {
    setNotice('请检查输入信息', 'error');
    return;
  }
  
  const success = await handleLogin(form.account, form.password, 'company');
  if (!success) {
    setNotice('登录失败，请检查账号密码', 'error');
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.company-login {
  background: linear-gradient(135deg, #0e6b52 0%, #1a8f6e 100%);
}

.auth-bg {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(circle at 20% 20%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(201, 152, 58, 0.1) 0%, transparent 50%),
    url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Ccircle cx='30' cy='30' r='1'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
  z-index: 0;
}

.auth-container {
  width: 100%;
  max-width: 440px;
  position: relative;
  z-index: 1;
  animation: slideInUp 0.8s ease-out;
}

.auth-card {
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.auth-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.4);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
  animation: fadeIn 0.6s ease-out 0.2s both;
}

.header-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #c9983a 0%, #e8b84b 100%);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(201, 152, 58, 0.3);
  animation: pulse 2s ease-in-out infinite;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: white;
  letter-spacing: 0.1em;
}

.auth-header .panel-title {
  font-size: 28px;
  color: #0e6b52;
  margin-bottom: 8px;
  font-weight: 700;
  font-family: 'Noto Sans SC', sans-serif;
}

.auth-header .muted {
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 24px;
  animation: fadeIn 0.6s ease-out 0.4s both;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  transition: all 0.3s ease;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  font-size: 16px;
  color: #94a3b8;
  transition: all 0.3s ease;
  z-index: 1; /* 新增这一行 */
}

.form-group input {
  padding: 14px 16px 14px 44px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: #f8fafc;
  color: #1e293b;
}

.form-group input:focus {
  outline: none;
  border-color: #c9983a;
  box-shadow: 0 0 0 3px rgba(201, 152, 58, 0.1);
  background: white;
  transform: translateY(-1px);
}

/* 在 CompanyLoginView.vue 中改为： */
.form-group:focus-within .input-icon {
  color: #c9983a;
  transform: scale(1.1);
}

.form-group input.invalid {
  border-color: #ef4444;
  background: #fef2f2;
}

.form-group input.invalid + .input-icon {
  color: #ef4444;
}

.error-message {
  font-size: 12px;
  color: #ef4444;
  margin-top: 2px;
  animation: slideInLeft 0.3s ease-out;
}

.status-line {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  animation: slideInUp 0.4s ease-out;
}

.status-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.status-icon.success {
  background: #10b981;
  color: white;
}

.status-icon.error {
  background: #ef4444;
  color: white;
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
  animation: fadeIn 0.6s ease-out 0.6s both;
}

.btn-block {
  width: 100%;
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #c9983a 0%, #e8b84b 100%);
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(201, 152, 58, 0.3);
}

.btn-block:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(201, 152, 58, 0.4);
  background: linear-gradient(135deg, #b8872e 0%, #d7a73e 100%);
}

.btn-block:disabled {
  background: #e5d19e;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 4px 12px rgba(201, 152, 58, 0.3); }
  50% { box-shadow: 0 6px 16px rgba(201, 152, 58, 0.5); }
}

@keyframes slideInUp {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.auth-links {
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.text-link {
  color: #c9983a;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  transition: all 0.3s ease;
  position: relative;
}

.text-link:hover {
  color: #b8872e;
}

.text-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: #c9983a;
  transition: width 0.3s ease;
}

.text-link:hover::after {
  width: 100%;
}

.auth-footer {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
  animation: fadeIn 0.6s ease-out 0.8s both;
}

.footer-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.footer-item:hover {
  color: #64748b;
  transform: translateY(-1px);
}

.footer-icon {
  font-size: 14px;
}

.footer-divider {
  color: #e2e8f0;
  font-size: 10px;
}

@media (max-width: 480px) {
  .auth-card {
    padding: 30px 20px;
  }
  
  .header-logo {
    padding: 12px;
    gap: 8px;
  }
  
  .logo-text {
    font-size: 16px;
  }
  
  .auth-header .panel-title {
    font-size: 24px;
  }
  
  .form-group input {
    padding: 12px 14px 12px 40px;
  }
  
  .btn-block {
    padding: 14px;
  }
  
  .footer-info {
    flex-direction: column;
    gap: 8px;
  }
  
  .footer-divider {
    display: none;
  }
}
</style>

