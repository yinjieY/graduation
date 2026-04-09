<template>
  <Layout role="company">
    <div class="auth-application">
      <div class="page-header">
        <h1 class="page-title">企业认证申请</h1>
        <p class="page-subtitle">提交企业资质认证申请</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>认证信息</h3>
        </div>
        <div class="form-group">
          <input v-model="applyForm.companyName" placeholder="企业名称" />
          <input v-model="applyForm.remark" placeholder="申请备注（可选）" />
        </div>
        <div class="form-actions">
          <button class="btn-primary" :disabled="loading" @click="onApply">提交申请</button>
          <button class="btn-secondary" :disabled="loading" @click="onQueryStatus">查询认证状态</button>
        </div>
        <div class="status-info">
          状态：<span :class="{ 'status-pending': statusText.includes('PENDING'), 'status-approved': statusText.includes('APPROVED') }">
            {{ statusText }}
          </span>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { queryCompanyStatus, submitCompanyApply } from '../../api/auth';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const statusText = ref('未查询');
const applyForm = reactive({ companyName: '', remark: '' });
const token = ref(getToken('company'));

// 从localStorage获取登录时的用户信息，包括companyId
const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
const companyId = ref(userInfo.companyId || '');

async function onApply() {
  loading.value = true;
  try {
    // 提交申请时，companyId应该从登录信息中获取，而不是用户输入
    const formData = {
      companyId: companyId.value,
      companyName: applyForm.companyName,
      remark: applyForm.remark
    };
    const res = await submitCompanyApply(formData, token.value);
    if (res.code === 200 && res.data) {
      statusText.value = res.data.statusText || 'PENDING';
    }
  } catch (error) {
    console.error('提交认证申请失败:', error);
  } finally {
    loading.value = false;
  }
}

async function onQueryStatus() {
  if (!companyId.value) {
    statusText.value = '请先登录获取企业信息';
    return;
  }
  loading.value = true;
  try {
    const res = await queryCompanyStatus(companyId.value, token.value);
    if (res.code === 200 && res.data) {
      statusText.value = `${res.data.statusText || ''} ${res.data.remark || ''}`.trim();
    }
  } catch (error) {
    console.error('查询认证状态失败:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-application {
  width: 100%;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 16px;
  color: #64748b;
  margin: 0;
}

.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.form-group input {
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-group input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-primary {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  padding: 10px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-info {
  font-size: 14px;
  color: #64748b;
}

.status-pending {
  color: #f59e0b;
  font-weight: 600;
}

.status-approved {
  color: #10b981;
  font-weight: 600;
}

@media (max-width: 768px) {
  .card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
}
</style>