<template>
  <Layout role="admin">
    <div class="enterprise-review">
      <div class="page-header">
        <h1 class="page-title">企业审查</h1>
        <p class="page-subtitle">审核企业资质认证申请</p>
      </div>

      <div v-if="error" class="status-line error">
        {{ error }}
      </div>

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
            <thead><tr><th>企业ID</th><th>企业名</th><th>申请人</th><th>备注</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="pendingList.length === 0"><td colspan="5" class="empty-state">暂无待审企业</td></tr>
            <tr v-for="item in pendingList" :key="item.companyId">
              <td>{{ item.companyId }}</td>
              <td class="company-name">{{ item.companyName }}</td>
              <td>{{ item.applyBy }}</td>
              <td class="remark">{{ item.remark }}</td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="review(item.companyId, true)">通过</button>
                <button class="btn-secondary" :disabled="loading" @click="review(item.companyId, false)">拒绝</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getPending, reviewCompany } from '../../api/auth';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const pendingList = ref([]);
const error = ref('');
const token = ref(getToken('admin'));

async function loadPending() {
  loading.value = true;
  error.value = '';
  try {
    const res = await getPending(token.value);
    pendingList.value = res.data || [];
  } catch (err) {
    console.error('加载待审企业失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function review(companyId, approved) {
  loading.value = true;
  try {
    await reviewCompany(companyId, approved, token.value);
    await loadPending();
  } catch (error) {
    console.error('审核企业失败:', error);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadPending();
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

.company-name {
  font-weight: 500;
  color: #1e293b;
}

.remark {
  max-width: 200px;
  white-space: normal;
  word-break: break-word;
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

@media (max-width: 768px) {
  .card {
    padding: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .actions {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>