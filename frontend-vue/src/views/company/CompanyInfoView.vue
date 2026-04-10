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
            v-model="formData.contactPhone"
            label="联系人"
            placeholder="请输入联系电话"
            :error="errors.contactPhone"
            required
          />
          <BaseInput
            v-model="formData.level"
            label="企业等级"
            placeholder="请输入企业等级（如 A/B）"
            :error="errors.level"
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
import { getToken } from '../../api/session';

const router = useRouter();
const api = useApi();
const { showSuccess, showError } = useNotification();

const loading = ref(false);
const formData = reactive({
  name: '',
  address: '',
  contactPhone: '',
  level: ''
});

const errors = reactive({
  name: '',
  address: '',
  contactPhone: '',
  level: ''
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
  if (!formData.contactPhone) {
    errors.contactPhone = '请输入联系电话';
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
    await api.updateCompanyInfo({
      name: formData.name,
      address: formData.address,
      contactPhone: formData.contactPhone,
      level: formData.level
    }, token);
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
    const token = getToken('company');
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