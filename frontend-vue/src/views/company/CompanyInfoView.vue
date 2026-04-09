<template>
  <Layout :role="'company'">
    <div class="company-info">
      <h1 class="page-title">企业信息管理</h1>
      <BaseForm @submit="handleSubmit">
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
            v-model="formData.contact"
            label="联系人"
            placeholder="请输入联系人姓名"
            :error="errors.contact"
            required
          />
          <BaseInput
            v-model="formData.phone"
            label="联系电话"
            placeholder="请输入联系电话"
            :error="errors.phone"
            required
          />
          <BaseInput
            v-model="formData.licenseNumber"
            label="营业执照号"
            placeholder="请输入营业执照号"
            :error="errors.licenseNumber"
            required
          />
          <BaseInput
            v-model="formData.productionStandard"
            label="生产标准"
            placeholder="请输入生产标准"
            :error="errors.productionStandard"
            required
          />
        </div>
        <div class="form-actions">
          <BaseButton type="secondary" @click="handleCancel">取消</BaseButton>
          <BaseButton type="primary" :loading="loading" @click="handleSubmit">保存修改</BaseButton>
        </div>
      </BaseForm>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Layout from '../../components/Layout.vue';
import BaseForm from '../../components/BaseForm.vue';
import BaseInput from '../../components/BaseInput.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';

const router = useRouter();
const api = useApi();
const { showSuccess, showError } = useNotification();

const loading = ref(false);
const formData = reactive({
  name: '',
  address: '',
  contact: '',
  phone: '',
  licenseNumber: '',
  productionStandard: ''
});

const errors = reactive({
  name: '',
  address: '',
  contact: '',
  phone: '',
  licenseNumber: '',
  productionStandard: ''
});

const validateForm = () => {
  let isValid = true;
  
  // 重置错误
  Object.keys(errors).forEach(key => {
    errors[key] = '';
  });
  
  // 验证字段
  if (!formData.name) {
    errors.name = '请输入企业名称';
    isValid = false;
  }
  if (!formData.address) {
    errors.address = '请输入企业地址';
    isValid = false;
  }
  if (!formData.contact) {
    errors.contact = '请输入联系人';
    isValid = false;
  }
  if (!formData.phone) {
    errors.phone = '请输入联系电话';
    isValid = false;
  }
  if (!formData.licenseNumber) {
    errors.licenseNumber = '请输入营业执照号';
    isValid = false;
  }
  if (!formData.productionStandard) {
    errors.productionStandard = '请输入生产标准';
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
    const token = localStorage.getItem('company_token');
    await api.updateCompanyInfo(formData, token);
    showSuccess('企业信息更新成功');
  } catch (error) {
    showError(handleApiError(error));
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
    const token = localStorage.getItem('company_token');
    const data = await api.getCompanyInfo(token);
    Object.assign(formData, data);
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadCompanyInfo();
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
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
}
</style>