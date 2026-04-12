<template>
  <Layout role="admin">
    <div class="batch-review-management">
      <div class="page-header">
        <h1 class="page-title">批次审核管理</h1>
        <p class="page-subtitle">审核企业提交的批次申请</p>
      </div>

      <div v-if="error" class="status-line error">
        {{ error }}
      </div>

      <!-- 查询表单 -->
      <div class="card">
        <div class="card-header">
          <h3>批次查询</h3>
        </div>
        <div class="query-form">
          <input v-model="queryForm.batchId" placeholder="批次ID" class="query-input" />
          <input v-model="queryForm.companyId" placeholder="企业ID" class="query-input" />
          <input v-model="queryForm.batchName" placeholder="批次名称" class="query-input" />
          <button class="btn-primary" :disabled="loading" @click="searchBatches">查询</button>
        </div>
      </div>

      <!-- 所有批次列表 -->
      <div class="card">
        <div class="card-header">
          <h3>所有批次列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadAllBatches">
            <span v-if="loading" class="loading-spinner"></span>
            刷新批次列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>批次ID</th><th>企业ID</th><th>批次名称</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="allBatches.length === 0"><td colspan="5" class="empty-state">暂无批次数据</td></tr>
            <tr v-for="item in allBatches" :key="item.batchId">
              <td>{{ item.batchId }}</td>
              <td>{{ item.companyId }}</td>
              <td class="batch-name">{{ item.batchName }}</td>
              <td>
                <span :class="{ 
                  'status-pending': item.status === 'PENDING', 
                  'status-approved': item.status === 'APPROVED', 
                  'status-rejected': item.status === 'REJECTED' 
                }">
                  {{ item.status }}
                </span>
              </td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="viewBatchDetailFromAll(item.batchId)">查看详情</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 待审核批次列表 -->
      <div class="card">
        <div class="card-header">
          <h3>待审核批次列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadPendingBatches">
            <span v-if="loading" class="loading-spinner"></span>
            刷新待审列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>批次ID</th><th>企业ID</th><th>批次名称</th><th>生产时间</th><th>数量</th><th>申请时间</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="pendingBatches.length === 0"><td colspan="7" class="empty-state">暂无待审批次</td></tr>
            <tr v-for="item in pendingBatches" :key="item.batchId">
              <td>{{ item.batchId }}</td>
              <td>{{ item.companyId }}</td>
              <td class="batch-name">{{ item.batchName }}</td>
              <td>{{ item.productionDate }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ item.createdAt }}</td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="reviewBatch(item.batchId, true)">通过</button>
                <button class="btn-secondary" :disabled="loading" @click="openRejectDialog(item.batchId)">拒绝</button>
                <button class="btn-secondary" :disabled="loading" @click="viewBatchDetailFromPending(item.batchId)">查看详情</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 拒绝原因对话框 -->
      <div v-if="showRejectDialog" class="dialog-overlay" @click="closeRejectDialog">
        <div class="dialog-content" @click.stop>
          <h2>拒绝原因</h2>
          <textarea v-model="rejectComment" placeholder="请输入拒绝原因" rows="4" class="comment-input"></textarea>
          <div class="form-actions">
            <button class="btn-secondary" @click="closeRejectDialog">取消</button>
            <button class="btn-primary" :disabled="loading" @click="confirmReject">确定</button>
          </div>
        </div>
      </div>

      <!-- 详情对话框 -->
      <div v-if="showDetailDialog" class="dialog-overlay" @click="closeDetailDialog">
        <div class="dialog-content" @click.stop>
          <h2>批次详情</h2>
          <div class="detail-content">
            <div class="detail-item">
              <label>批次ID：</label>
              <span>{{ currentBatchDetail?.batchId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业ID：</label>
              <span>{{ currentBatchDetail?.companyId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>批次名称：</label>
              <span>{{ currentBatchDetail?.batchName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>生产时间：</label>
              <span>{{ currentBatchDetail?.productionDate || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>生产数量：</label>
              <span>{{ currentBatchDetail?.quantity || 0 }}</span>
            </div>
            <div class="detail-item">
              <label>申请时间：</label>
              <span>{{ currentBatchDetail?.createdAt || '-' }}</span>
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
import { onMounted, ref } from 'vue';
import { getPendingBatches, reviewBatch as apiReviewBatch, getProductBatchList } from '../../api/trace';
import { getToken } from '../../api/session';
import { clearCache, clearCacheByUrl } from '../../api/http';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const pendingBatches = ref([]);
const allBatches = ref([]);
const error = ref('');
const token = ref(getToken('admin'));
const showRejectDialog = ref(false);
const showDetailDialog = ref(false);
const currentBatchId = ref('');
const rejectComment = ref('');
const currentBatchDetail = ref(null);
const queryForm = ref({
  batchId: '',
  companyId: '',
  batchName: ''
});

function toViewModel(item) {
  return {
    batchId: item.batchId || '-',
    companyId: item.companyId || '-',
    batchName: item.productionStandard || item.batchId || '-',
    productionDate: item.productionDate ? String(item.productionDate).slice(0, 10) : '-',
    quantity: item.totalQuantity || 0,
    createdAt: item.createdAt ? String(item.createdAt).slice(0, 16) : '-',
    status: item.reviewStatus || item.status || 'PENDING'
  };
}

async function loadPendingBatches() {
  loading.value = true;
  error.value = '';
  try {
    // 清除缓存，确保数据实时更新
    clearCache();
    
    const res = await getPendingBatches(token.value);
    pendingBatches.value = (res.data || []).map(toViewModel);
  } catch (err) {
    console.error('加载待审批次失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function loadAllBatches() {
  loading.value = true;
  error.value = '';
  try {
    // 清除缓存，确保数据实时更新
    clearCache();
    
    const res = await getProductBatchList(token.value);
    allBatches.value = (res.data || []).map(toViewModel);
  } catch (err) {
    console.error('加载批次列表失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function searchBatches() {
  loading.value = true;
  error.value = '';
  try {
    // 清除缓存，确保数据实时更新
    clearCache();
    
    const res = await getProductBatchList(token.value);
    let batchList = (res.data || []).map(toViewModel);
    
    // 过滤批次
    if (queryForm.value.batchId) {
      batchList = batchList.filter(batch => String(batch.batchId || '').includes(queryForm.value.batchId));
    }
    if (queryForm.value.companyId) {
      batchList = batchList.filter(batch => String(batch.companyId || '').includes(queryForm.value.companyId));
    }
    if (queryForm.value.batchName) {
      batchList = batchList.filter(batch => String(batch.batchName || '').includes(queryForm.value.batchName));
    }
    
    allBatches.value = batchList;
  } catch (err) {
    console.error('查询批次失败:', err);
    error.value = `查询失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function reviewBatch(batchId, approved) {
  loading.value = true;
  try {
    await apiReviewBatch(batchId, approved ? 'APPROVED' : 'REJECTED', approved ? '' : rejectComment.value, token.value);
    // 清除缓存，确保重新获取最新数据
    clearCache();
    await loadPendingBatches();
    await loadAllBatches();
    error.value = approved ? '审核通过成功' : '审核拒绝成功';
    closeRejectDialog();
  } catch (err) {
    error.value = `审核失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

function openRejectDialog(batchId) {
  currentBatchId.value = batchId;
  rejectComment.value = '';
  showRejectDialog.value = true;
}

function closeRejectDialog() {
  showRejectDialog.value = false;
  currentBatchId.value = '';
  rejectComment.value = '';
}

async function confirmReject() {
  if (currentBatchId.value) {
    await reviewBatch(currentBatchId.value, false);
  }
}

function viewBatchDetailFromAll(batchId) {
  const hit = allBatches.value.find((item) => String(item.batchId) === String(batchId));
  if (hit) {
    currentBatchDetail.value = hit;
    showDetailDialog.value = true;
  }
}

function viewBatchDetailFromPending(batchId) {
  const hit = pendingBatches.value.find((item) => String(item.batchId) === String(batchId));
  if (hit) {
    currentBatchDetail.value = hit;
    showDetailDialog.value = true;
  }
}

function closeDetailDialog() {
  showDetailDialog.value = false;
  currentBatchDetail.value = null;
}

onMounted(async () => {
  await Promise.all([loadPendingBatches(), loadAllBatches()]);
});
</script>

<style scoped>
.batch-review-management {
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

.status-line {
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.status-line.error {
  background-color: #fee2e2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  padding: 24px;
  margin-bottom: 24px;
}

.query-form {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.query-input {
  flex: 1;
  min-width: 200px;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.query-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
  transition: all 0.2s ease;
}

.btn-refresh:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-refresh:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

th {
  text-align: left;
  padding: 12px 16px;
  background: #f8fafc;
  font-weight: 600;
  color: #334155;
  border-bottom: 2px solid #e2e8f0;
}

 td {
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
  color: #475569;
}

.empty-state {
  text-align: center;
  color: #94a3b8;
  padding: 48px 24px;
  font-style: italic;
}

.batch-name {
  font-weight: 500;
  color: #1e293b;
}

.status-pending {
  background: #fef3c7;
  color: #d97706;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-approved {
  background: #d1fae5;
  color: #059669;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-rejected {
  background: #fee2e2;
  color: #dc2626;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-primary {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  padding: 6px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: white;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
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
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.dialog-content h2 {
  font-size: 18px;
  font-weight: 700;
  color: #334155;
  margin: 0 0 24px 0;
}

.comment-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  min-height: 100px;
  margin-bottom: 20px;
}

.comment-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.detail-content {
  margin-bottom: 24px;
}

.detail-item {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.detail-item label {
  width: 100px;
  font-weight: 600;
  color: #334155;
  flex-shrink: 0;
}

.detail-item span {
  flex: 1;
  color: #475569;
  word-break: break-word;
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
  
  .query-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .query-input {
    min-width: auto;
  }
  
  .actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  
  .dialog-content {
    width: 95%;
    padding: 20px;
  }
}
</style>