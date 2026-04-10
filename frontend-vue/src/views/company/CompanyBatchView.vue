<template>
  <Layout :role="'company'">
    <div class="batch-management">
      <div class="page-header">
        <h1 class="page-title">生产批次管理</h1>
        <BaseButton type="primary" icon="➕" @click="openCreateDialog">新增批次</BaseButton>
      </div>
      
      <div class="batch-list">
        <div class="batch-item" v-for="batch in batches" :key="batch.batchId" :class="{ 'batch-pending': batch.status === 'PENDING', 'batch-approved': batch.status === 'APPROVED', 'batch-rejected': batch.status === 'REJECTED' }">
          <div class="batch-info">
            <h3 class="batch-name">{{ batch.batchName }}</h3>
            <div class="batch-meta">
              <span class="meta-item">生产时间: {{ batch.productionDate }}</span>
              <span class="meta-item">数量: {{ batch.quantity }}</span>
              <span class="meta-item" :class="{ 'status-pending': batch.status === 'PENDING', 'status-approved': batch.status === 'APPROVED', 'status-rejected': batch.status === 'REJECTED' }">状态: {{ batch.status }}</span>
            </div>
            <div class="batch-description">{{ batch.description }}</div>
            <div class="batch-review-info" v-if="batch.status === 'PENDING'">
              <span class="review-status">审核状态: 待审核</span>
              <span class="review-tip">请等待管理员审核通过后才能使用该批次</span>
            </div>
            <div class="batch-review-info" v-else-if="batch.status === 'APPROVED'">
              <span class="review-status">审核状态: 已通过</span>
              <span class="review-tip">该批次已审核通过，可以使用</span>
            </div>
            <div class="batch-review-info" v-else-if="batch.status === 'REJECTED'">
              <span class="review-status">审核状态: 已拒绝</span>
              <span class="review-tip">该批次审核被拒绝，请修改后重新提交</span>
            </div>
          </div>
          <div class="batch-actions">
            <BaseButton type="primary" size="small" @click="handleEditBatch(batch)">编辑</BaseButton>
            <BaseButton type="error" size="small" @click="handleDeleteBatch(batch.batchId)">删除</BaseButton>
            <BaseButton type="success" size="small" @click="handleGenerateQrCodes(batch.batchId)" :disabled="batch.status !== 'APPROVED'">生成码</BaseButton>
          </div>
        </div>
      </div>
      
      <!-- 新增/编辑批次对话框 -->
      <div v-if="showAddBatchDialog" class="dialog-overlay" @click="showAddBatchDialog = false">
        <div class="dialog-content" @click.stop>
          <h2>{{ editingBatch ? '编辑批次' : '新增批次' }}</h2>
          <BaseForm @submit="handleSaveBatch">
            <BaseInput
              v-model="formData.batchName"
              label="批次名称"
              placeholder="请输入批次名称"
              :error="errors.batchName"
              required
            />
            <BaseInput
              v-model="formData.productionDate"
              label="生产时间"
              type="date"
              :error="errors.productionDate"
              required
            />
            <BaseInput
              v-model="formData.quantity"
              label="数量"
              type="number"
              placeholder="请输入数量"
              :error="errors.quantity"
              required
            />
            <BaseInput
              v-model="formData.description"
              label="描述"
              placeholder="请输入批次描述"
              :error="errors.description"
            />
            <div class="form-actions">
              <BaseButton type="secondary" @click="showAddBatchDialog = false">取消</BaseButton>
              <BaseButton type="primary" :loading="loading" @click="handleSaveBatch">保存</BaseButton>
            </div>
          </BaseForm>
        </div>
      </div>
      
      <!-- 生成二维码对话框 -->
      <div v-if="showQrCodeDialog" class="dialog-overlay" @click="showQrCodeDialog = false">
        <div class="dialog-content" @click.stop>
          <h2>生成溯源码</h2>
          <BaseForm @submit="handleGenerateQrCodesSubmit">
            <BaseInput
              v-model="qrCodeCount"
              label="生成数量"
              type="number"
              placeholder="请输入生成数量"
              :error="errors.qrCodeCount"
              required
            />
            <div class="form-actions">
              <BaseButton type="secondary" @click="showQrCodeDialog = false">取消</BaseButton>
              <BaseButton type="primary" :loading="generatingQrCodes" @click="handleGenerateQrCodesSubmit">生成</BaseButton>
            </div>
          </BaseForm>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import BaseForm from '../../components/BaseForm.vue';
import BaseInput from '../../components/BaseInput.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';
import { clearCacheByUrl } from '../../api/http';

const api = useApi();
const { showSuccess, showError } = useNotification();

const loading = ref(false);
const generatingQrCodes = ref(false);
const showAddBatchDialog = ref(false);
const showQrCodeDialog = ref(false);
const editingBatch = ref(null);
const currentBatchId = ref(null);
const qrCodeCount = ref(100);

const batches = ref([]);

const formData = reactive({
  batchId: '',
  batchName: '',
  productionDate: '',
  quantity: '',
  description: ''
});

const errors = reactive({
  batchName: '',
  productionDate: '',
  quantity: '',
  description: '',
  qrCodeCount: ''
});

const validateForm = () => {
  let isValid = true;
  
  // 重置错误
  Object.keys(errors).forEach(key => {
    errors[key] = '';
  });
  
  // 验证字段
  if (!formData.batchName) {
    errors.batchName = '请输入批次名称';
    isValid = false;
  }
  if (!formData.productionDate) {
    errors.productionDate = '请输入生产时间';
    isValid = false;
  }
  if (!formData.quantity) {
    errors.quantity = '请输入数量';
    isValid = false;
  }
  
  return isValid;
};

const handleSaveBatch = async () => {
  if (!validateForm()) {
    return;
  }
  
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const payload = {
      batchId: formData.batchId || undefined,
      productionDate: formData.productionDate ? `${formData.productionDate}T00:00:00` : '',
      ingredients: formData.description || formData.batchName,
      productionStandard: formData.batchName,
      totalQuantity: Number(formData.quantity)
    };

    if (editingBatch.value) {
      await api.updateProductBatch(payload, token);
      showSuccess('批次更新成功');
    } else {
      await api.createProductBatch(payload, token);
      showSuccess('批次创建成功');
    }
    
    resetForm();
    showAddBatchDialog.value = false;
    await loadBatches();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const handleEditBatch = (batch) => {
  editingBatch.value = batch;
  Object.assign(formData, {
    batchId: batch.batchId,
    batchName: batch.batchName,
    productionDate: batch.productionDate,
    quantity: batch.quantity,
    description: batch.description
  });
  showAddBatchDialog.value = true;
};

const handleDeleteBatch = async (batchId) => {
  if (!confirm('确定要删除这个批次吗？')) {
    return;
  }
  
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    await api.deleteProductBatch(batchId, token);
    showSuccess('批次删除成功');
    await loadBatches();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const handleGenerateQrCodes = (batchId) => {
  currentBatchId.value = batchId;
  showQrCodeDialog.value = true;
};

const handleGenerateQrCodesSubmit = async () => {
  if (!qrCodeCount.value || qrCodeCount.value <= 0) {
    errors.qrCodeCount = '请输入有效的生成数量';
    return;
  }
  
  try {
    generatingQrCodes.value = true;
    const token = localStorage.getItem('company_token');
    await api.generateQrCodes(currentBatchId.value, qrCodeCount.value, token);
    showSuccess('溯源码生成成功');
    showQrCodeDialog.value = false;
    await loadBatches();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    generatingQrCodes.value = false;
  }
};

const loadBatches = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    
    clearCacheByUrl('/trace/batch/list');
    clearCacheByUrl('/trace/qs/list');
    
    const data = await api.getProductBatchList(token);
    const qrCodeList = await api.getQrCodeList(token);
    const qrCodeBatchIds = new Set(qrCodeList.map(item => item.batchId));
    
    batches.value = (data || []).map((item) => ({
      batchId: item.batchId,
      batchName: item.productionStandard || item.batchId,
      productionDate: item.productionDate ? String(item.productionDate).slice(0, 10) : '',
      quantity: item.totalQuantity,
      description: item.ingredients || '',
      status: qrCodeBatchIds.has(item.batchId) ? (item.status || 'ACTIVE') : 'PENDING'
    }));
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

function resetForm() {
  editingBatch.value = null;
  Object.assign(formData, {
    batchId: '',
    batchName: '',
    productionDate: '',
    quantity: '',
    description: ''
  });
}

function openCreateDialog() {
  resetForm();
  showAddBatchDialog.value = true;
}

onMounted(() => {
  loadBatches();
});
</script>

<style scoped>
.batch-management {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #334155;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.batch-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #3b82f6;
}

.batch-info {
  flex: 1;
}

.batch-name {
  font-size: 16px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 8px 0;
}

.batch-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 14px;
  color: #64748b;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.batch-description {
  font-size: 14px;
  color: #64748b;
  line-height: 1.4;
}

.batch-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 审核状态样式 */
.batch-pending {
  border-color: #f59e0b;
  background-color: #fffbeb;
}

.batch-approved {
  border-color: #10b981;
  background-color: #ecfdf5;
}

.batch-rejected {
  border-color: #ef4444;
  background-color: #fef2f2;
}

.status-pending {
  color: #f59e0b;
  font-weight: 600;
}

.status-approved {
  color: #10b981;
  font-weight: 600;
}

.status-rejected {
  color: #ef4444;
  font-weight: 600;
}

.batch-review-info {
  margin-top: 8px;
  padding: 8px;
  border-radius: 4px;
  font-size: 14px;
}

.batch-pending .batch-review-info {
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
}

.batch-approved .batch-review-info {
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
}

.batch-rejected .batch-review-info {
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
}

.review-status {
  display: block;
  font-weight: 600;
  margin-bottom: 4px;
}

.review-tip {
  display: block;
  font-size: 12px;
  opacity: 0.8;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.dialog-content h2 {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
  margin: 0 0 24px 0;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

@media (max-width: 768px) {
  .batch-management {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .batch-item {
    flex-direction: column;
    gap: 12px;
  }
  
  .batch-actions {
    align-self: flex-start;
  }
  
  .batch-meta {
    flex-direction: column;
    gap: 4px;
  }
}
</style>