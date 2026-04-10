<template>
  <Layout role="company">
    <div class="auth-application">
      <div class="page-header">
        <h1 class="page-title">企业认证申请</h1>
        <p class="page-subtitle">提交企业资质认证申请</p>
      </div>

      <!-- 认证状态展示 -->
      <div class="status-card" :class="statusClass">
        <div class="status-header">
          <h3>认证状态</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadAuthStatus">
            <span v-if="loading" class="loading-spinner"></span>
            刷新状态
          </button>
        </div>
        <div class="status-content">
          <div class="status-icon" :class="statusIconClass">
            {{ statusIcon }}
          </div>
          <div class="status-details">
            <h4 class="status-title">{{ statusTitle }}</h4>
            <p class="status-description">{{ statusDescription }}</p>
            <p class="status-remark" v-if="statusRemark">{{ statusRemark }}</p>
          </div>
        </div>
      </div>

      <!-- 认证申请表单 -->
      <div class="card" v-if="showApplyForm">
        <div class="card-header">
          <h3>认证信息</h3>
        </div>
        <div class="form-group">
          <div class="form-item">
            <label>企业名称</label>
            <input v-model="applyForm.companyName" placeholder="请输入企业名称" required />
          </div>
          <div class="form-item">
            <label>统一社会信用代码</label>
            <input v-model="applyForm.socialCreditCode" placeholder="请输入统一社会信用代码" required />
          </div>
          <div class="form-item">
            <label>法人姓名</label>
            <input v-model="applyForm.legalPerson" placeholder="请输入法人姓名" required />
          </div>
          <div class="form-item">
            <label>联系电话</label>
            <input v-model="applyForm.contactPhone" placeholder="请输入联系电话" required />
          </div>
          <div class="form-item">
            <label>申请备注（可选）</label>
            <textarea v-model="applyForm.remark" placeholder="请输入申请备注" rows="3"></textarea>
          </div>
        </div>
        <div class="form-actions">
          <button class="btn-primary" :disabled="loading" @click="onApply">提交申请</button>
        </div>
      </div>

      <!-- 认证成功信息 -->
      <div class="card" v-if="authStatus === 'APPROVED'">
        <div class="card-header">
          <h3>认证信息</h3>
        </div>
        <div class="auth-info">
          <div class="info-item">
            <label>企业ID：</label>
            <span>{{ companyId }}</span>
          </div>
          <div class="info-item">
            <label>企业名称：</label>
            <span>{{ companyName }}</span>
          </div>
          <div class="info-item">
            <label>认证时间：</label>
            <span>{{ authTime }}</span>
          </div>
        </div>
      </div>

      <!-- 认证失败信息 -->
      <div class="card" v-if="authStatus === 'REJECTED'">
        <div class="card-header">
          <h3>认证失败原因</h3>
        </div>
        <div class="reject-info">
          <p class="reject-reason">{{ rejectReason }}</p>
          <button class="btn-primary" :disabled="loading" @click="resetForm">重新申请</button>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { queryCompanyStatus, submitCompanyApply } from '../../api/auth';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const authStatus = ref('NOT_APPLIED'); // NOT_APPLIED, PENDING, APPROVED, REJECTED
const statusText = ref('未申请');
const statusRemark = ref('');
const companyId = ref('');
const companyName = ref('');
const authTime = ref('');
const rejectReason = ref('');

const applyForm = reactive({
  companyName: '',
  socialCreditCode: '',
  legalPerson: '',
  contactPhone: '',
  remark: ''
});

const token = ref(getToken('company'));

function getCurrentCompanyId() {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    return String(userInfo.companyId || '').trim();
  } catch (error) {
    return '';
  }
}

// 计算状态相关的属性
const statusTitle = computed(() => {
  switch (authStatus.value) {
    case 'NOT_APPLIED':
      return '未申请认证';
    case 'PENDING':
      return '审核中';
    case 'APPROVED':
      return '认证成功';
    case 'REJECTED':
      return '认证失败';
    default:
      return '未知状态';
  }
});

const statusDescription = computed(() => {
  switch (authStatus.value) {
    case 'NOT_APPLIED':
      return '您尚未提交企业认证申请，请点击下方按钮提交申请';
    case 'PENDING':
      return '您的认证申请正在审核中，预计1-3个工作日完成审核';
    case 'APPROVED':
      return '您的企业认证已通过，现在可以使用所有功能';
    case 'REJECTED':
      return '您的认证申请未通过，请查看失败原因并重新提交申请';
    default:
      return '';
  }
});

const statusClass = computed(() => {
  switch (authStatus.value) {
    case 'NOT_APPLIED':
      return 'status-not-applied';
    case 'PENDING':
      return 'status-pending';
    case 'APPROVED':
      return 'status-approved';
    case 'REJECTED':
      return 'status-rejected';
    default:
      return '';
  }
});

const statusIcon = computed(() => {
  switch (authStatus.value) {
    case 'NOT_APPLIED':
      return '📋';
    case 'PENDING':
      return '⏳';
    case 'APPROVED':
      return '✅';
    case 'REJECTED':
      return '❌';
    default:
      return '❓';
  }
});

const statusIconClass = computed(() => {
  switch (authStatus.value) {
    case 'NOT_APPLIED':
      return 'icon-not-applied';
    case 'PENDING':
      return 'icon-pending';
    case 'APPROVED':
      return 'icon-approved';
    case 'REJECTED':
      return 'icon-rejected';
    default:
      return '';
  }
});

const showApplyForm = computed(() => {
  return authStatus.value === 'NOT_APPLIED' || authStatus.value === 'REJECTED';
});

// 加载认证状态
async function loadAuthStatus() {
  loading.value = true;
  try {
    const currentCompanyId = getCurrentCompanyId();
    if (!currentCompanyId) {
      authStatus.value = 'NOT_APPLIED';
      statusRemark.value = '当前账号未绑定 companyId，请联系管理员处理';
      return;
    }

    const res = await queryCompanyStatus(currentCompanyId, token.value);
    if (res.code === 200 && res.data) {
      authStatus.value = res.data.statusText || 'NOT_APPLIED';
      statusRemark.value = res.data.remark || '';
      companyId.value = res.data.companyId || '';
      companyName.value = res.data.companyName || '';
      authTime.value = res.data.reviewTime || '';
      rejectReason.value = res.data.remark || '';
    } else {
      authStatus.value = 'NOT_APPLIED';
      statusRemark.value = res.msg || '企业认证记录不存在';
    }
  } catch (error) {
    console.error('查询认证状态失败:', error);
    // 如果查询失败，默认为未申请状态
    authStatus.value = 'NOT_APPLIED';
  } finally {
    loading.value = false;
  }
}

// 提交认证申请
async function onApply() {
  if (!applyForm.companyName) {
    alert('请填写企业名称');
    return;
  }

  loading.value = true;
  try {
    const formData = {
      companyName: applyForm.companyName,
      remark: applyForm.remark
    };
    const res = await submitCompanyApply(formData, token.value);
    if (res.code === 200 && res.data) {
      authStatus.value = res.data.statusText || 'PENDING';
      statusRemark.value = res.data.remark || '';
      companyId.value = res.data.companyId || '';
      companyName.value = res.data.companyName || applyForm.companyName;
      alert('认证申请提交成功，正在审核中');
    } else {
      alert(res.msg || '认证申请提交失败');
    }
  } catch (error) {
    console.error('提交认证申请失败:', error);
    alert('提交认证申请失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

// 重置表单
function resetForm() {
  Object.keys(applyForm).forEach(key => {
    applyForm[key] = '';
  });
  authStatus.value = 'NOT_APPLIED';
}

// 页面加载时自动查询认证状态
onMounted(() => {
  loadAuthStatus();
});
</script>

<style scoped>
.auth-application {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  margin-bottom: 16px;
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

/* 状态卡片样式 */
.status-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 24px;
  border-left: 4px solid;
}

.status-not-applied {
  border-left-color: #94a3b8;
}

.status-pending {
  border-left-color: #f59e0b;
}

.status-approved {
  border-left-color: #10b981;
}

.status-rejected {
  border-left-color: #ef4444;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

.btn-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.status-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.status-icon {
  font-size: 48px;
  flex-shrink: 0;
}

.icon-not-applied {
  color: #94a3b8;
}

.icon-pending {
  color: #f59e0b;
}

.icon-approved {
  color: #10b981;
}

.icon-rejected {
  color: #ef4444;
}

.status-details {
  flex: 1;
}

.status-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.status-description {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.status-remark {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
  font-style: italic;
}

/* 卡片样式 */
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 24px;
}

.card-header {
  margin-bottom: 20px;
}

.card-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

/* 表单样式 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.form-item input,
.form-item textarea {
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.form-item input:focus,
.form-item textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-item textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  gap: 12px;
}

.btn-primary {
  padding: 12px 24px;
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

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 认证信息样式 */
.auth-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  gap: 12px;
  align-items: center;
}

.info-item label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  min-width: 100px;
}

.info-item span {
  font-size: 14px;
  color: #64748b;
  flex: 1;
}

/* 认证失败信息样式 */
.reject-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reject-reason {
  font-size: 14px;
  color: #ef4444;
  line-height: 1.4;
  margin: 0;
  padding: 16px;
  background: #fef2f2;
  border-radius: 8px;
  border: 1px solid #fee2e2;
}

@media (max-width: 768px) {
  .auth-application {
    padding: 16px;
  }
  
  .status-card,
  .card {
    padding: 16px;
  }
  
  .status-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .status-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .status-icon {
    font-size: 32px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
  
  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .info-item label {
    min-width: auto;
  }
}
</style>