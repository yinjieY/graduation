<template>
  <Layout role="admin">
    <div class="enterprise-review">
      <div class="page-header">
        <h1 class="page-title">企业管理</h1>
        <p class="page-subtitle">管理企业信息和审核认证申请</p>
      </div>

      <div v-if="error" class="status-line error">
        {{ error }}
      </div>

      <!-- 企业查询 -->
      <div class="card">
        <div class="card-header">
          <h3>企业查询</h3>
        </div>
        <div class="query-form">
          <input v-model="queryForm.companyId" placeholder="企业ID" class="query-input" />
          <input v-model="queryForm.companyName" placeholder="企业名称" class="query-input" />
          <button class="btn-primary" :disabled="loading" @click="searchCompanies">查询</button>
        </div>
      </div>

      <!-- 所有企业列表 -->
      <div class="card">
        <div class="card-header">
          <h3>所有企业列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadAllCompanies">
            <span v-if="loading" class="loading-spinner"></span>
            刷新企业列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>企业ID</th><th>企业名</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="companies.length === 0"><td colspan="4" class="empty-state">暂无企业数据</td></tr>
            <tr v-for="item in companies" :key="item.companyId">
              <td>{{ item.companyId }}</td>
              <td class="company-name">{{ item.companyName }}</td>
              <td>
                <span :class="{ 'status-pending': item.status === 'PENDING', 'status-approved': item.status === 'APPROVED', 'status-rejected': item.status === 'REJECTED' }">
                  {{ item.status }}
                </span>
              </td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="viewCompanyDetail(item.companyId)">查看详情</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 待审企业列表 -->
      <div class="card">
        <div class="card-header">
          <h3>待审企业列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadPending">
            <span v-if="loading" class="loading-spinner"></span>
            刷新待审列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="pendingList.length === 0"><td colspan="4" class="empty-state">暂无待审企业</td></tr>
            <tr v-for="item in pendingList" :key="item.companyId">
              <td>{{ item.companyId }}</td>
              <td class="company-name">{{ item.companyName }}</td>
              <td>{{ item.applyBy || '-' }}</td>
              <td class="actions">
                <button class="btn-detail" :disabled="loading" @click="viewPendingDetail(item)">详情</button>
                <button class="btn-primary" :disabled="loading" @click="review(item.companyId, true)">通过</button>
                <button class="btn-secondary" :disabled="loading" @click="review(item.companyId, false)">拒绝</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 企业详情对话框 -->
      <div v-if="showDetailDialog" class="dialog-overlay" @click="closeDetailDialog">
        <div class="dialog-content" @click.stop>
          <h2>{{ isPendingDetail ? '待审企业详情' : '企业详情' }}</h2>
          <div class="detail-content">
            <div class="detail-item">
              <label>企业ID：</label>
              <span>{{ currentCompanyDetail?.companyId || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业名称：</label>
              <span>{{ currentCompanyDetail?.name || currentCompanyDetail?.companyName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业地址：</label>
              <span>{{ currentCompanyDetail?.address || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>企业邮箱：</label>
              <span>{{ currentCompanyDetail?.email || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>联系电话：</label>
              <span>{{ currentCompanyDetail?.contactPhone || '-' }}</span>
            </div>
            <div v-if="isPendingDetail" class="detail-item">
              <label>申请人：</label>
              <span>{{ currentCompanyDetail?.applyBy || '-' }}</span>
            </div>
            <div v-if="isPendingDetail" class="detail-item">
              <label>申请时间：</label>
              <span>{{ formatTime(currentCompanyDetail?.applyTime) }}</span>
            </div>
            <div v-if="isPendingDetail" class="detail-item">
              <label>备注：</label>
              <span>{{ currentCompanyDetail?.remark || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>状态：</label>
              <span :class="{ 
                'status-pending': currentCompanyDetail?.reviewStatus === 0 || currentCompanyDetail?.status === 'PENDING', 
                'status-approved': currentCompanyDetail?.reviewStatus === 1 || currentCompanyDetail?.status === 'APPROVED', 
                'status-rejected': currentCompanyDetail?.reviewStatus === 2 || currentCompanyDetail?.status === 'REJECTED' 
              }">
                {{ currentCompanyDetail?.statusText || currentCompanyDetail?.status || '-' }}
              </span>
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
import { getPending, reviewCompany } from '../../api/auth';
import { getCompanyList } from '../../api/trace';
import { getToken } from '../../api/session';
import { clearCache, clearCacheByUrl } from '../../api/http';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const pendingList = ref([]);
const companies = ref([]);
const error = ref('');
const token = ref(getToken('admin'));
const queryForm = ref({ companyId: '', companyName: '' });
const showDetailDialog = ref(false);
const currentCompanyDetail = ref(null);
const isPendingDetail = ref(false);

async function loadPending() {
  loading.value = true;
  error.value = '';
  try {
    // 清除所有缓存，确保重新获取最新数据
    clearCache();
    const res = await getPending(token.value);
    pendingList.value = res.data || [];
  } catch (err) {
    console.error('加载待审企业失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function loadAllCompanies() {
  loading.value = true;
  error.value = '';
  try {
    // 清除所有缓存，确保重新获取最新数据
    clearCache();
    const res = await getCompanyList(token.value);
    companies.value = (res.data || []).map((item) => ({
      ...item,
      companyName: item.name || item.companyName || '-',
      status: Number(item.status) === 1 ? 'APPROVED' : 'REJECTED'
    }));
  } catch (err) {
    console.error('加载企业列表失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function searchCompanies() {
  loading.value = true;
  error.value = '';
  try {
    // 清除所有缓存，确保重新获取最新数据
    clearCache();
    const res = await getCompanyList(token.value);
    let companyList = (res.data || []).map((item) => ({
      ...item,
      companyName: item.name || item.companyName || '-',
      status: Number(item.status) === 1 ? 'APPROVED' : 'REJECTED'
    }));

    // 前端过滤
    if (queryForm.value.companyId) {
      companyList = companyList.filter(company => String(company.companyId || '').includes(queryForm.value.companyId));
    }
    if (queryForm.value.companyName) {
      companyList = companyList.filter(company => String(company.name || company.companyName || '').includes(queryForm.value.companyName));
    }
    
    companies.value = companyList;
  } catch (err) {
    console.error('查询企业失败:', err);
    error.value = `查询失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function review(companyId, approved) {
  loading.value = true;
  try {
    await reviewCompany(companyId, approved, token.value);
    // 清除所有缓存，确保重新获取最新数据
    clearCache();
    await loadPending();
    await loadAllCompanies();
  } catch (error) {
    console.error('审核企业失败:', error);
  } finally {
    loading.value = false;
  }
}

function viewCompanyDetail(companyId) {
  const hit = companies.value.find((item) => String(item.companyId) === String(companyId));
  if (hit) {
    currentCompanyDetail.value = hit;
    isPendingDetail.value = false;
    showDetailDialog.value = true;
  }
}

function viewPendingDetail(item) {
  currentCompanyDetail.value = item;
  isPendingDetail.value = true;
  showDetailDialog.value = true;
}

function closeDetailDialog() {
  showDetailDialog.value = false;
  currentCompanyDetail.value = null;
  isPendingDetail.value = false;
}

function formatTime(time) {
  if (!time) return '-';
  const date = new Date(time);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

onMounted(async () => {
  await Promise.all([loadPending(), loadAllCompanies()]);
});
</script>

<style scoped>
.enterprise-review {
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

/* 查询表单样式 */
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

.company-name {
  font-weight: 500;
  color: #1e293b;
}

.remark {
  max-width: 200px;
  white-space: normal;
  word-break: break-word;
}

/* 状态标签样式 */
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

.btn-detail {
  padding: 6px 12px;
  border: 1px solid #8b5cf6;
  border-radius: 6px;
  background: white;
  color: #8b5cf6;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-detail:hover:not(:disabled) {
  background: #f5f3ff;
  border-color: #7c3aed;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
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
}
</style>