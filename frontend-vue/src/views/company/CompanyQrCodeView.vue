<template>
  <Layout role="company">
    <div class="qrcode-management">
      <div class="page-header">
        <h1 class="page-title">溯源码管理</h1>
        <div class="header-actions">
          <button class="btn-refresh" :disabled="loading" @click="loadQrCodes">
            <span v-if="loading" class="loading-spinner"></span>
            刷新列表
          </button>
        </div>
      </div>

      <div v-if="error" class="status-line error">
        {{ error }}
      </div>

      <!-- 二维码查询 -->
      <div class="card">
        <div class="card-header">
          <h3>溯源码查询</h3>
        </div>
        <div class="query-form">
          <input v-model="queryForm.qsId" placeholder="溯源码ID" class="query-input" />
          <input v-model="queryForm.batchId" placeholder="批次ID" class="query-input" />
          <select v-model="queryForm.status" class="query-input">
            <option value="">所有状态</option>
            <option value="active">正常</option>
            <option value="invalid">已停用</option>
            <option value="frozen">已冻结</option>
          </select>
          <button class="btn-primary" :disabled="loading" @click="searchQrCodes">查询</button>
        </div>
      </div>

      <!-- 溯源码列表 -->
      <div class="card">
        <div class="card-header">
          <h3>我的溯源码</h3>
          <span class="card-count">共 {{ filteredQrCodes.length }} 条记录</span>
        </div>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>溯源码ID</th>
                <th>批次ID</th>
                <th>批次名称</th>
                <th>企业名称</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredQrCodes.length === 0">
                <td colspan="7" class="empty-state">暂无溯源码数据</td>
              </tr>
              <tr v-for="item in filteredQrCodes" :key="item.qsId">
                <td>{{ item.qsId }}</td>
                <td>{{ item.batchId }}</td>
                <td>{{ item.batchName || '-' }}</td>
                <td>{{ item.companyName || '-' }}</td>
                <td>
                  <span :class="{
                    'status-active': item.status === 'active',
                    'status-disabled': item.status === 'invalid',
                    'status-frozen': item.status === 'frozen'
                  }">
                    {{ getStatusText(item.status) }}
                  </span>
                </td>
                <td>{{ item.createdAt }}</td>
                <td class="actions">
                  <button 
                    class="btn-primary" 
                    :disabled="loading || item.status === 'active' || item.status === 'frozen'" 
                    @click="handleEnable(item.qsId)">
                    启用
                  </button>
                  <button 
                    class="btn-secondary" 
                    :disabled="loading || item.status === 'invalid'" 
                    @click="handleDisable(item.qsId)">
                    停用
                  </button>
                  <button 
                    class="btn-secondary" 
                    :disabled="loading" 
                    @click="viewDetail(item.qsId, item.companyName)">
                    查看详情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 溯源码详情对话框 -->
      <div v-if="showDetailDialog" class="dialog-overlay" @click="closeDetailDialog">
        <div class="dialog-content" @click.stop>
          <h2>溯源码详情</h2>
          <div class="detail-content">
            <div class="detail-item">
              <label>溯源码ID：</label>
              <span>{{ currentDetail?.qsId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>批次ID：</label>
              <span>{{ currentDetail?.batchId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业ID：</label>
              <span>{{ currentDetail?.companyId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业名称：</label>
              <span>{{ companyName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>状态：</label>
              <span>{{ getStatusText(currentDetail?.status) || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>创建时间：</label>
              <span>{{ currentDetail?.createdAt || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>更新时间：</label>
              <span>{{ currentDetail?.updatedAt || '-' }}</span>
            </div>
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="closeDetailDialog">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import Layout from '../../components/Layout.vue';
import { getQrCodeList, updateQrCodeStatus, getQrCodeDetail } from '../../api/trace';
import { clearCacheByUrl } from '../../api/http';
import { getCompanyToken } from '../../api/auth';

const loading = ref(false);
const error = ref('');
const qrCodes = ref([]);
const showDetailDialog = ref(false);
const currentDetail = ref(null);
const companyName = ref('');

const queryForm = ref({
  qsId: '',
  batchId: '',
  status: ''
});

const filteredQrCodes = computed(() => {
  let result = qrCodes.value;
  
  if (queryForm.value.qsId) {
    result = result.filter(item => item.qsId.includes(queryForm.value.qsId));
  }
  
  if (queryForm.value.batchId) {
    result = result.filter(item => item.batchId.includes(queryForm.value.batchId));
  }
  
  if (queryForm.value.status) {
    const targetStatus = queryForm.value.status.toUpperCase();
    result = result.filter(item => String(item.status).toUpperCase() === targetStatus);
  }
  
  return result;
});

function getStatusText(status) {
  const statusMap = {
    'active': '正常',
    'invalid': '已停用',
    'frozen': '已冻结'
  };
  return statusMap[status] || status;
}

const loadQrCodes = async () => {
  try {
    loading.value = true;
    error.value = '';
    const token = getCompanyToken();
    const response = await getQrCodeList(token);
    qrCodes.value = response.data || [];
  } catch (err) {
    error.value = err.message || '加载溯源码列表失败';
    console.error('加载溯源码列表失败:', err);
  } finally {
    loading.value = false;
  }
};

const searchQrCodes = async () => {
  await loadQrCodes();
};

const handleEnable = async (qsId) => {
  try {
    loading.value = true;
    const token = getCompanyToken();
    await updateQrCodeStatus(qsId, 'active', token);
    clearCacheByUrl('/trace/qs/list');
    await loadQrCodes();
    alert('溯源码已启用');
  } catch (err) {
    alert('启用失败: ' + (err.message || '未知错误'));
    console.error('启用溯源码失败:', err);
  } finally {
    loading.value = false;
  }
};

const handleDisable = async (qsId) => {
  if (!confirm('确定要停用该溯源码吗？')) {
    return;
  }
  
  try {
    loading.value = true;
    const token = getCompanyToken();
    await updateQrCodeStatus(qsId, 'invalid', token);
    clearCacheByUrl('/trace/qs/list');
    await loadQrCodes();
    alert('溯源码已停用');
  } catch (err) {
    alert('停用失败: ' + (err.message || '未知错误'));
    console.error('停用溯源码失败:', err);
  } finally {
    loading.value = false;
  }
};

const viewDetail = async (qsId, companyNameValue) => {
  try {
    loading.value = true;
    const token = getCompanyToken();
    const response = await getQrCodeDetail(qsId, token);
    const detailData = response.data || response;
    currentDetail.value = detailData.qsCode || detailData;
    
    // 使用传入的企业名称或从详情数据中获取
    if (companyNameValue) {
      companyName.value = companyNameValue;
    } else if (detailData.company?.name) {
      companyName.value = detailData.company.name;
    } else if (currentDetail.value?.companyId) {
      companyName.value = currentDetail.value.companyId;
    }
    
    showDetailDialog.value = true;
  } catch (err) {
    alert('获取详情失败: ' + (err.message || '未知错误'));
    console.error('获取溯源码详情失败:', err);
  } finally {
    loading.value = false;
  }
};

const closeDetailDialog = () => {
  showDetailDialog.value = false;
  currentDetail.value = null;
  companyName.value = '';
};

onMounted(() => {
  loadQrCodes();
});
</script>

<style scoped>
.qrcode-management {
  padding: 24px;
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
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-refresh {
  padding: 8px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-refresh:hover:not(:disabled) {
  background: #f1f5f9;
}

.btn-refresh:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.status-line {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.status-line.error {
  background: #fee2e2;
  color: #dc2626;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.card-count {
  font-size: 14px;
  color: #64748b;
}

.query-form {
  padding: 16px 20px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.query-input {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  min-width: 160px;
}

.btn-primary {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 8px 16px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
  font-size: 14px;
}

.btn-secondary:hover:not(:disabled) {
  background: #f1f5f9;
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

table th,
table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 40px;
}

.status-active {
  color: #22c55e;
  font-weight: 500;
}

.status-disabled {
  color: #f59e0b;
  font-weight: 500;
}

.status-frozen {
  color: #ef4444;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 8px;
}

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
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  min-width: 400px;
  max-width: 90%;
  max-height: 80vh;
  overflow-y: auto;
}

.dialog-content h2 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.detail-content {
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.detail-item label {
  font-weight: 500;
  color: #475569;
  min-width: 100px;
}

.detail-item span {
  color: #1e293b;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .qrcode-management {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .query-form {
    flex-direction: column;
  }

  .query-input {
    min-width: 100%;
  }

  .dialog-content {
    min-width: 90%;
    padding: 16px;
  }
}
</style>
