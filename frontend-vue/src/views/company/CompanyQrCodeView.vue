<template>
  <Layout :role="'company'">
    <div class="qr-code-management">
      <h1 class="page-title">溯源码管理</h1>
      
      <div class="batch-selector">
        <label for="batch-select">选择批次：</label>
        <select id="batch-select" v-model="selectedBatchId" @change="loadQrCodes">
          <option value="">请选择批次</option>
          <option v-for="batch in batches" :key="batch.id" :value="batch.id">
            {{ batch.batchName }}
          </option>
        </select>
      </div>
      
      <div class="qr-code-list" v-if="selectedBatchId">
        <div class="qr-code-item" v-for="qrCode in qrCodes" :key="qrCode.id">
          <div class="qr-code-info">
            <div class="qr-code-id">{{ qrCode.id }}</div>
            <div class="qr-code-status" :class="`status-${qrCode.status}`">
              {{ qrCode.status === 'ACTIVE' ? '激活' : qrCode.status === 'INACTIVE' ? '未激活' : '冻结' }}
            </div>
          </div>
          <div class="qr-code-actions">
            <BaseButton 
              :type="qrCode.status === 'ACTIVE' ? 'warning' : 'success'"
              size="small"
              @click="handleUpdateStatus(qrCode.id, qrCode.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')"
            >
              {{ qrCode.status === 'ACTIVE' ? '停用' : '激活' }}
            </BaseButton>
            <BaseButton type="error" size="small" @click="handleUpdateStatus(qrCode.id, 'FROZEN')">
              冻结
            </BaseButton>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-state">
        <div class="empty-icon">📱</div>
        <div class="empty-text">请选择一个批次查看溯源码</div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import BaseButton from '../../components/BaseButton.vue';
import { useApi, handleApiError } from '../../composables/useApi';
import { useNotification } from '../../composables/useNotification';

const api = useApi();
const { showSuccess, showError } = useNotification();

const loading = ref(false);
const selectedBatchId = ref('');
const batches = ref([]);
const qrCodes = ref([]);

const handleUpdateStatus = async (qrCodeId, status) => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    await api.updateQrCodeStatus(qrCodeId, status, token);
    showSuccess('状态更新成功');
    loadQrCodes();
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const loadBatches = async () => {
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const data = await api.getProductBatchList(token);
    batches.value = data;
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

const loadQrCodes = async () => {
  if (!selectedBatchId.value) {
    qrCodes.value = [];
    return;
  }
  
  try {
    loading.value = true;
    const token = localStorage.getItem('company_token');
    const data = await api.getQrCodeList(selectedBatchId.value, token);
    qrCodes.value = data;
  } catch (error) {
    showError(handleApiError(error));
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadBatches();
});
</script>

<style scoped>
.qr-code-management {
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

.batch-selector {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.batch-selector label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.batch-selector select {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  min-width: 200px;
}

.qr-code-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.qr-code-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.qr-code-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #3b82f6;
}

.qr-code-info {
  flex: 1;
}

.qr-code-id {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  margin-bottom: 4px;
  word-break: break-all;
}

.qr-code-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
  font-weight: 500;
}

.status-ACTIVE {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}

.status-INACTIVE {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
}

.status-FROZEN {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.qr-code-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 16px;
  color: #64748b;
}

@media (max-width: 768px) {
  .qr-code-management {
    padding: 16px;
  }
  
  .page-title {
    font-size: 18px;
  }
  
  .batch-selector {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  
  .batch-selector select {
    width: 100%;
  }
  
  .qr-code-list {
    grid-template-columns: 1fr;
  }
  
  .qr-code-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .qr-code-actions {
    align-self: flex-end;
  }
}
</style>