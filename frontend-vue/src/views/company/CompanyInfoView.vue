<template>
  <Layout :role="'company'">
    <div class="company-info">
      <h1 class="page-title">企业认证申请</h1>
      
      <div class="auth-section" v-if="authStatus">
        <div class="auth-header">
          <h3>企业认证状态</h3>
          <span :class="['auth-status', authStatus.toLowerCase()]">{{ authStatusDisplay }}</span>
        </div>
        <div class="auth-info">
          <p>当前认证状态：<strong>{{ authStatusDisplay }}</strong></p>
          <p v-if="authStatus === 'APPROVED'" class="approved-hint">
            ✅ 企业已通过认证，企业信息已归档，如需修改请联系管理员
          </p>
        </div>
      </div>

      <div v-if="authStatus === 'APPROVED'" class="read-only-section">
        <div v-if="!isEditing" class="read-only-grid">
          <div class="read-only-item">
            <label>企业名称</label>
            <span>{{ formData.name || '-' }}</span>
          </div>
          <div class="read-only-item">
            <label>企业地址</label>
            <span>{{ formData.address || '-' }}</span>
          </div>
          <div class="read-only-item">
            <label>联系电话</label>
            <span>{{ formData.contactPhone || '-' }}</span>
          </div>
          <div class="read-only-item">
            <label>纬度</label>
            <span>{{ formData.lat || '-' }}</span>
          </div>
          <div class="read-only-item">
            <label>经度</label>
            <span>{{ formData.lng || '-' }}</span>
          </div>
        </div>
        
        <BaseForm v-if="isEditing" @submit="handleSubmitModify">
          <div class="form-grid">
            <BaseInput
              v-model="formData.name"
              label="企业名称"
              placeholder="请输入企业名称"
              :error="errors.name"
              required
            />
            <BaseInput
              v-model="formData.address"
              label="企业地址"
              placeholder="请输入企业地址"
              :error="errors.address"
              required
            />
            <BaseInput
              v-model="formData.contactPhone"
              label="联系电话"
              placeholder="请输入联系电话"
              :error="errors.contactPhone"
              required
            />
            <BaseInput
              v-model="formData.lat"
              type="number"
              label="纬度"
              placeholder="请输入企业所在纬度（如：27.03）"
              :error="errors.lat"
              required
            />
            <BaseInput
              v-model="formData.lng"
              type="number"
              label="经度"
              placeholder="请输入企业所在经度（如：113.32）"
              :error="errors.lng"
              required
            />
            <div class="form-row full-width">
              <BaseInput
                v-model="formData.remark"
                type="textarea"
                label="申请备注"
                placeholder="请输入申请备注（选填）"
                :rows="3"
              />
            </div>
          </div>
        </BaseForm>
        
        <div class="read-only-actions">
          <BaseButton 
            v-if="!isEditing"
            type="primary" 
            :loading="loading" 
            @click="startEditing"
          >
            申请修改企业信息
          </BaseButton>
          <template v-else>
            <BaseButton 
              type="default" 
              @click="cancelEditing"
            >
              取消
            </BaseButton>
            <BaseButton 
              type="primary" 
              :loading="loading" 
              @click="handleSubmitModify"
            >
              提交修改申请
            </BaseButton>
          </template>
        </div>
      </div>

      <BaseForm v-else @submit="handleSubmit">
        <div class="form-grid">
          <BaseInput
            v-model="formData.name"
            label="企业名称"
            placeholder="请输入企业名称"
            :error="errors.name"
            required
          />
          <BaseInput
            v-model="formData.address"
            label="企业地址"
            placeholder="请输入企业地址"
            :error="errors.address"
            required
          />
          <BaseInput
            v-model="formData.contactPhone"
            label="联系电话"
            placeholder="请输入联系电话"
            :error="errors.contactPhone"
            required
          />
          <BaseInput
            v-model="formData.lat"
            label="纬度"
            placeholder="请输入企业所在纬度（如：27.03）"
            :error="errors.lat"
            type="number"
            step="0.01"
            required
          />
          <BaseInput
            v-model="formData.lng"
            label="经度"
            placeholder="请输入企业所在经度（如：113.32）"
            :error="errors.lng"
            type="number"
            step="0.01"
            required
          />
          <div class="form-hint">
            <span>📍 经纬度获取提示：可通过百度地图、高德地图等工具查询企业位置的经纬度</span>
          </div>
          <BaseInput
            v-model="applyRemark"
            label="申请备注"
            placeholder="请输入申请备注（选填）"
            class="remark-input"
          />
        </div>
        <div class="form-actions">
          <BaseButton type="secondary" @click="handleCancel">取消</BaseButton>
          <BaseButton 
            type="primary" 
            :loading="loading" 
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            {{ authStatus === 'PENDING' ? '重新提交申请' : '提交认证申请' }}
          </BaseButton>
        </div>
        <div v-if="submitNotice" :class="['submit-notice', submitNoticeType]">{{ submitNotice }}</div>
      </BaseForm>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import Layout from '../../components/Layout.vue';
import BaseForm from '../../components/BaseForm.vue';
import BaseInput from '../../components/BaseInput.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';
import { getToken } from '../../api/session';

const router = useRouter();
const api = useApi();
const { showSuccess, showError } = useNotification();

const loading = ref(false);
const authStatus = ref('');
const applyRemark = ref('');
const submitNotice = ref('');
const submitNoticeType = ref('info');
const isEditing = ref(false);
const originalData = reactive({
  name: '',
  address: '',
  contactPhone: '',
  lat: '',
  lng: ''
});

const formData = reactive({
  name: '',
  address: '',
  contactPhone: '',
  lat: '',
  lng: ''
});

const errors = reactive({
  name: '',
  address: '',
  contactPhone: '',
  lat: '',
  lng: ''
});

const authStatusDisplay = computed(() => {
  const statusMap = {
    'APPROVED': '已通过',
    'PENDING': '审核中',
    'REJECTED': '已拒绝',
    'UNAPPLIED': '未申请'
  };
  return statusMap[authStatus.value] || authStatus.value;
});

const canSubmit = computed(() => {
  if (!formData.name || !formData.address || !formData.contactPhone) return false;
  if (!formData.lat || !formData.lng) return false;
  return true;
});

const validateForm = () => {
  let isValid = true;
  
  Object.keys(errors).forEach(key => {
    errors[key] = '';
  });
  
  if (!formData.name) {
    errors.name = '请输入企业名称';
    isValid = false;
  }
  if (!formData.address) {
    errors.address = '请输入企业地址';
    isValid = false;
  }
  if (!formData.contactPhone) {
    errors.contactPhone = '请输入联系电话';
    isValid = false;
  }
  if (!formData.lat) {
    errors.lat = '请输入纬度';
    isValid = false;
  }
  if (!formData.lng) {
    errors.lng = '请输入经度';
    isValid = false;
  }
  
  return isValid;
};

const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }
  
  try {
    loading.value = true;
    const token = getToken('company');
    const companyId = getCurrentCompanyId();
    if (!companyId) {
      throw new Error('当前账号未绑定企业ID');
    }
    
    await api.submitCompanyApply({
      companyId,
      companyName: formData.name,
      address: formData.address,
      contactPhone: formData.contactPhone,
      lat: parseFloat(formData.lat),
      lng: parseFloat(formData.lng),
      remark: applyRemark.value
    }, token);
    
    authStatus.value = 'PENDING';
    applyRemark.value = '';
    submitNotice.value = '认证申请已提交，请等待管理员审核';
    submitNoticeType.value = 'success';
  } catch (error) {
    submitNotice.value = handleApiError(error);
    submitNoticeType.value = 'error';
  } finally {
    loading.value = false;
  }
};

const startEditing = () => {
  originalData.name = formData.name;
  originalData.address = formData.address;
  originalData.contactPhone = formData.contactPhone;
  originalData.lat = formData.lat;
  originalData.lng = formData.lng;
  isEditing.value = true;
};

const cancelEditing = () => {
  formData.name = originalData.name;
  formData.address = originalData.address;
  formData.contactPhone = originalData.contactPhone;
  formData.lat = originalData.lat;
  formData.lng = originalData.lng;
  isEditing.value = false;
};

const handleSubmitModify = async () => {
  if (!validateForm()) {
    return;
  }
  
  const confirmResult = confirm('确定要提交修改申请吗？修改申请需要管理员审核通过后才能生效。');
  if (!confirmResult) {
    return;
  }
  
  try {
    loading.value = true;
    const token = getToken('company');
    const companyId = getCurrentCompanyId();
    if (!companyId) {
      throw new Error('当前账号未绑定企业ID');
    }
    
    await api.submitCompanyApply({
      companyId,
      companyName: formData.name,
      address: formData.address,
      contactPhone: formData.contactPhone,
      lat: parseFloat(formData.lat),
      lng: parseFloat(formData.lng),
      remark: formData.remark || '申请修改企业信息'
    }, token);
    
    authStatus.value = 'PENDING';
    isEditing.value = false;
    formData.remark = '';
    submitNotice.value = '修改申请已提交，请等待管理员审核';
    submitNoticeType.value = 'success';
  } catch (error) {
    submitNotice.value = handleApiError(error);
    submitNoticeType.value = 'error';
  } finally {
    loading.value = false;
  }
};

const handleCancel = () => {
  router.push('/company/dashboard');
};

const loadCompanyInfo = async () => {
  try {
    loading.value = true;
    const token = getToken('company');
    const data = await api.getCompanyInfo(token);
    Object.assign(formData, data);
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const queryAuthStatus = async () => {
  try {
    const token = getToken('company');
    const companyId = getCurrentCompanyId();
    if (!companyId) {
      authStatus.value = 'UNAPPLIED';
      return;
    }
    const res = await api.queryCompanyStatus(companyId, token, { noCache: true });
    if (res && res.statusText) {
      authStatus.value = res.statusText;
    } else {
      authStatus.value = 'UNAPPLIED';
    }
  } catch (error) {
    console.error('查询认证状态失败:', error);
    authStatus.value = 'UNAPPLIED';
  }
};

const getCurrentCompanyId = () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    return String(userInfo.companyId || '').trim();
  } catch (error) {
    return '';
  }
};

onMounted(() => {
  loadCompanyInfo();
  queryAuthStatus();
});
</script>

<style scoped>
.company-info {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-section {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.auth-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.auth-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0;
}

.auth-status {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.auth-status.approved {
  background-color: #d1fae5;
  color: #065f46;
}

.auth-status.pending {
  background-color: #fef3c7;
  color: #b45309;
}

.auth-status.rejected {
  background-color: #fee2e2;
  color: #dc2626;
}

.auth-status.unapplied {
  background-color: #e0e7ff;
  color: #4338ca;
}

.auth-info {
  margin: 0;
}

.auth-info p {
  font-size: 14px;
  color: #64748b;
  margin: 4px 0;
}

.approved-hint {
  color: #065f46 !important;
  font-weight: 500;
}

.read-only-section {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
}

.read-only-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.read-only-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.read-only-item label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.read-only-item span {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.read-only-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.form-hint {
  grid-column: span 2;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
  color: #64748b;
}

.remark-input {
  grid-column: span 2;
}

.form-actions {
  display: flex;
  gap: 24px;
  margin-top: 32px;
  justify-content: flex-end;
}

.submit-notice {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-top: 16px;
}

.submit-notice.success {
  background-color: #d1fae5;
  color: #065f46;
}

.submit-notice.error {
  background-color: #fee2e2;
  color: #dc2626;
}

.submit-notice.warning {
  background-color: #fef3c7;
  color: #b45309;
}

.submit-notice.info {
  background-color: #e0e7ff;
  color: #4338ca;
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .company-info {
    padding: 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
  
  .auth-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .read-only-grid {
    grid-template-columns: 1fr;
  }
  
  .form-hint {
    grid-column: span 1;
  }
  
  .remark-input {
    grid-column: span 1;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .form-actions button {
    width: 100%;
  }
}
</style>