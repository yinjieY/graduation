<template>
  <Layout :role="'company'">
    <div class="batch-management">
      <div class="page-header">
        <h1 class="page-title">生产批次管理</h1>
        <div class="header-actions">
          <BaseButton type="secondary" icon="🔄" @click="loadBatches" :disabled="loading">刷新</BaseButton>
          <BaseButton type="primary" icon="➕" @click="openCreateDialog">新增批次</BaseButton>
        </div>
      </div>
      
      <div class="batch-list">
        <div class="batch-item" v-for="batch in batches" :key="batch.batchId" :class="{ 'batch-draft': batch.status === 'DRAFT' || !batch.status, 'batch-pending': batch.status === 'PENDING', 'batch-approved': batch.status === 'APPROVED', 'batch-rejected': batch.status === 'REJECTED' }">
          <div class="batch-info">
            <h3 class="batch-name">{{ batch.batchName }}</h3>
            <div class="batch-meta">
              <span class="meta-item">批次ID: {{ batch.batchId }}</span>
              <span class="meta-item">生产时间: {{ batch.productionDate }}</span>
              <span class="meta-item">数量: {{ batch.quantity }}</span>
              <span class="meta-item" :class="{ 'status-draft': batch.status === 'DRAFT', 'status-pending': batch.status === 'PENDING', 'status-approved': batch.status === 'APPROVED', 'status-rejected': batch.status === 'REJECTED' }">状态: {{ getStatusText(batch.status) }}</span>
            </div>
            <div class="batch-description">{{ batch.description }}</div>
            <div class="batch-review-info" v-if="batch.status === 'DRAFT' || !batch.status">
              <span class="review-status">状态: 草稿</span>
              <span class="review-tip">请完善批次信息后申请审核</span>
            </div>
            <div class="batch-review-info" v-else-if="batch.status === 'PENDING'">
              <span class="review-status">审核状态: 待审核</span>
              <span class="review-tip">请等待管理员审核通过后才能使用该批次</span>
            </div>
            <div class="batch-review-info" v-else-if="batch.status === 'APPROVED'">
              <span class="review-status">审核状态: 已通过</span>
              <span class="review-tip">该批次已审核通过，可以生成二维码</span>
            </div>
            <div class="batch-review-info" v-else-if="batch.status === 'REJECTED'">
              <span class="review-status">审核状态: 已拒绝</span>
              <span class="review-tip">该批次审核被拒绝，请修改后重新提交</span>
              <span class="review-comment" v-if="batch.reviewComment">拒绝原因: {{ batch.reviewComment }}</span>
            </div>
          </div>
          <div class="batch-actions">
            <BaseButton type="primary" size="small" @click="handleEditBatch(batch)" :disabled="batch.status === 'PENDING' || batch.status === 'APPROVED'">编辑</BaseButton>
            <BaseButton type="error" size="small" @click="handleDeleteBatch(batch.batchId)" :disabled="batch.status === 'APPROVED'">删除</BaseButton>
            <BaseButton type="warning" size="small" @click="handleApplyReview(batch.batchId)" v-if="batch.status === 'DRAFT' || !batch.status">申请审核</BaseButton>
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
      
      <!-- 二维码预览对话框 -->
      <div v-if="showQrCodePreviewDialog" class="dialog-overlay" @click="showQrCodePreviewDialog = false">
        <div class="dialog-content qr-code-preview" @click.stop>
          <h2>二维码预览</h2>
          <div class="qr-code-list">
            <div v-for="(qrCode, index) in qrCodeData" :key="qrCode.qsId" class="qr-code-item">
              <div class="qr-code-image">
                <!-- 这里使用占位符，实际应该显示真实的二维码图片 -->
                <div class="qr-code-placeholder">
                  <span class="qr-code-icon">📱</span>
                  <span class="qr-code-text">{{ qrCode.qsId }}</span>
                </div>
              </div>
              <div class="qr-code-info">
                <div class="qr-code-id">ID: {{ qrCode.qsId }}</div>
                <div 
                  class="qr-code-status" 
                  :class="{
                    'status-active': qrCode.status === 'ACTIVE' || qrCode.status === 'active',
                    'status-disabled': qrCode.status === 'INVALID' || qrCode.status === 'invalid',
                    'status-frozen': qrCode.status === 'FROZEN' || qrCode.status === 'frozen'
                  }">
                  {{ getQrCodeStatusText(qrCode.status) }}
                </div>
              </div>
              <BaseButton type="primary" size="small" @click="downloadQrCode(qrCode.qsId)">下载</BaseButton>
            </div>
          </div>
          <div class="form-actions">
            <BaseButton type="secondary" @click="showQrCodePreviewDialog = false">关闭</BaseButton>
          </div>
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
const showQrCodePreviewDialog = ref(false);
const editingBatch = ref(null);

const batches = ref([]);
const qrCodeData = ref([]);
const batchQrCodeStatus = ref(new Map()); // 记录批次的二维码生成状态

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
  description: ''
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

const handleApplyReview = async (batchId) => {
  if (!confirm('确定要申请审核这个批次吗？申请后将无法修改批次信息')) {
    return;
  }
  
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    await api.applyForReview(batchId, token);
    showSuccess('申请审核成功，请等待管理员审核');
    await loadBatches();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const handleGenerateQrCodes = async (batchId) => {
  try {
    // 检查批次是否已经有二维码
    if (batchQrCodeStatus.value.has(batchId)) {
      // 直接展示二维码
      qrCodeData.value = batchQrCodeStatus.value.get(batchId);
      showQrCodePreviewDialog.value = true;
      return;
    }
    
    generatingQrCodes.value = true;
    const token = localStorage.getItem('company_token');
    
    // 获取批次信息，使用批次的总数量
    const batch = batches.value.find(b => b.batchId === batchId);
    if (!batch) {
      showError('批次信息不存在');
      return;
    }
    
    const quantity = batch.quantity || 0;
    if (quantity <= 0) {
      showError('批次数量必须大于0');
      return;
    }
    
    await api.generateQrCodes(batchId, quantity, token);
    showSuccess('溯源码生成成功');
    
    // 重新加载数据并展示二维码
    await loadBatches();
    if (batchQrCodeStatus.value.has(batchId)) {
      qrCodeData.value = batchQrCodeStatus.value.get(batchId);
      showQrCodePreviewDialog.value = true;
    }
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
    
    // 构建批次的二维码状态映射
    const batchQrCodeMap = new Map();
    qrCodeList.forEach(qrCode => {
      if (!batchQrCodeMap.has(qrCode.batchId)) {
        batchQrCodeMap.set(qrCode.batchId, []);
      }
      batchQrCodeMap.get(qrCode.batchId).push(qrCode);
    });
    
    // 更新批次二维码状态
    batchQrCodeStatus.value = batchQrCodeMap;
    
    batches.value = (data || []).map((item) => ({
      batchId: item.batchId,
      batchName: item.productionStandard || item.batchId,
      productionDate: item.productionDate ? String(item.productionDate).slice(0, 10) : '',
      quantity: item.totalQuantity,
      description: item.ingredients || '',
      status: item.reviewStatus || 'DRAFT',
      reviewComment: item.reviewComment || '',
      hasQrCodes: batchQrCodeMap.has(item.batchId)
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

function getStatusText(status) {
  const statusMap = {
    'DRAFT': '草稿',
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  };
  return statusMap[status] || '草稿';
}

function getQrCodeStatusText(status) {
  const statusMap = {
    'ACTIVE': '活跃',
    'active': '活跃',
    'INVALID': '已停用',
    'invalid': '已停用',
    'FROZEN': '已冻结',
    'frozen': '已冻结'
  };
  return statusMap[status] || '活跃';
}

function downloadQrCode(qsId) {
  // 构建二维码下载URL，使用正确的后端接口和端口
  const downloadUrl = `http://localhost:9090/trace/qs/image/${qsId}.png`;
  
  // 创建下载链接
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.setAttribute('download', `${qsId}.png`);
  link.setAttribute('target', '_blank');
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
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

.header-actions {
  display: flex;
  gap: 12px;
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
.batch-draft {
  border-color: #94a3b8;
  background-color: #f8fafc;
}

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

.status-draft {
  color: #64748b;
  font-weight: 600;
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

.batch-draft .batch-review-info {
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
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

.review-comment {
  display: block;
  font-size: 12px;
  margin-top: 4px;
  font-style: italic;
  color: #dc2626;
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

/* 二维码预览样式 */
.qr-code-preview {
  max-width: 800px;
  max-height: 600px;
  overflow-y: auto;
}

.qr-code-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.qr-code-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.qr-code-image {
  margin-bottom: 12px;
}

.qr-code-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.qr-code-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.qr-code-text {
  font-size: 12px;
  color: #64748b;
  text-align: center;
  word-break: break-all;
}

.qr-code-info {
  text-align: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.qr-code-id {
  font-weight: 600;
  color: #334155;
  margin-bottom: 4px;
}

.qr-code-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.qr-code-status.status-active {
  color: #10b981;
  background-color: #ecfdf5;
}

.qr-code-status.status-disabled {
  color: #f59e0b;
  background-color: #fffbeb;
}

.qr-code-status.status-frozen {
  color: #ef4444;
  background-color: #fef2f2;
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
  
  .qr-code-list {
    grid-template-columns: 1fr;
  }
  
  .qr-code-preview {
    max-width: 95%;
    max-height: 80vh;
  }
}
</style>