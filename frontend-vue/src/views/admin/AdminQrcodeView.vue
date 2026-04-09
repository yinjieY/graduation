<template>
  <Layout role="admin">
    <div class="qrcode-management">
      <div class="page-header">
        <h1 class="page-title">二维码管理</h1>
        <p class="page-subtitle">审核公司申请二维码并进行管理</p>
      </div>

      <div v-if="error" class="status-line error">
        {{ error }}
      </div>

      <!-- 二维码查询 -->
      <div class="card">
        <div class="card-header">
          <h3>二维码查询</h3>
        </div>
        <div class="query-form">
          <input v-model="queryForm.qrcodeId" placeholder="二维码ID" class="query-input" />
          <input v-model="queryForm.companyId" placeholder="企业ID" class="query-input" />
          <input v-model="queryForm.companyName" placeholder="企业名称" class="query-input" />
          <select v-model="queryForm.status" class="query-input">
            <option value="">所有状态</option>
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已拒绝</option>
            <option value="ACTIVE">已激活</option>
            <option value="FROZEN">已冻结</option>
          </select>
          <button class="btn-primary" :disabled="loading" @click="searchQrcodes">查询</button>
        </div>
      </div>

      <!-- 待审二维码列表 -->
      <div class="card">
        <div class="card-header">
          <h3>待审二维码列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadPendingQrcodes">
            <span v-if="loading" class="loading-spinner"></span>
            刷新待审列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>二维码ID</th><th>企业ID</th><th>企业名</th><th>申请时间</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="pendingQrcodes.length === 0"><td colspan="5" class="empty-state">暂无待审二维码</td></tr>
            <tr v-for="item in pendingQrcodes" :key="item.qrcodeId">
              <td>{{ item.qrcodeId }}</td>
              <td>{{ item.companyId }}</td>
              <td class="company-name">{{ item.companyName }}</td>
              <td>{{ item.applyTime }}</td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading" @click="reviewQrcode(item.qrcodeId, true)">通过</button>
                <button class="btn-secondary" :disabled="loading" @click="reviewQrcode(item.qrcodeId, false)">拒绝</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 所有二维码列表 -->
      <div class="card">
        <div class="card-header">
          <h3>所有二维码列表</h3>
          <button class="btn-refresh" :disabled="loading" @click="loadAllQrcodes">
            <span v-if="loading" class="loading-spinner"></span>
            刷新二维码列表
          </button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr><th>二维码ID</th><th>企业ID</th><th>企业名</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
            <tr v-if="qrcodes.length === 0"><td colspan="6" class="empty-state">暂无二维码数据</td></tr>
            <tr v-for="item in qrcodes" :key="item.qrcodeId">
              <td>{{ item.qrcodeId }}</td>
              <td>{{ item.companyId }}</td>
              <td class="company-name">{{ item.companyName }}</td>
              <td>
                <span :class="{
                  'status-pending': item.status === 'PENDING',
                  'status-approved': item.status === 'APPROVED',
                  'status-rejected': item.status === 'REJECTED',
                  'status-active': item.status === 'ACTIVE',
                  'status-frozen': item.status === 'FROZEN'
                }">
                  {{ item.status }}
                </span>
              </td>
              <td>{{ item.createTime }}</td>
              <td class="actions">
                <button class="btn-primary" :disabled="loading || item.status === 'FROZEN'" @click="freezeQrcode(item.qrcodeId, true)">冻结</button>
                <button class="btn-secondary" :disabled="loading || item.status !== 'FROZEN'" @click="freezeQrcode(item.qrcodeId, false)">解冻</button>
                <button class="btn-secondary" :disabled="loading" @click="viewQrcodeDetail(item.qrcodeId)">查看详情</button>
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
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const pendingQrcodes = ref([]);
const qrcodes = ref([]);
const error = ref('');
const token = ref(getToken('admin'));
const queryForm = ref({
  qrcodeId: '',
  companyId: '',
  companyName: '',
  status: ''
});

async function loadPendingQrcodes() {
  loading.value = true;
  error.value = '';
  try {
    // 假设后端有获取待审二维码的接口
    // 这里暂时使用模拟数据，实际项目中需要替换为真实接口
    // const res = await apiFetch('/qrcode/pending', { headers: authHeaders(token.value) });
    // pendingQrcodes.value = res.data || [];
    
    // 模拟数据
    pendingQrcodes.value = [
      { qrcodeId: 'Q1001', companyId: '1001', companyName: '攸县香干食品有限公司', applyTime: '2026-04-01 10:00:00' },
      { qrcodeId: 'Q1002', companyId: '1003', companyName: '株洲市豆制品有限公司', applyTime: '2026-04-02 14:30:00' }
    ];
  } catch (err) {
    console.error('加载待审二维码失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function loadAllQrcodes() {
  loading.value = true;
  error.value = '';
  try {
    // 假设后端有获取所有二维码的接口
    // 这里暂时使用模拟数据，实际项目中需要替换为真实接口
    // const res = await apiFetch('/qrcode/all', { headers: authHeaders(token.value) });
    // qrcodes.value = res.data || [];
    
    // 模拟数据
    qrcodes.value = [
      { qrcodeId: 'Q1001', companyId: '1001', companyName: '攸县香干食品有限公司', status: 'PENDING', createTime: '2026-04-01 10:00:00' },
      { qrcodeId: 'Q1002', companyId: '1003', companyName: '株洲市豆制品有限公司', status: 'PENDING', createTime: '2026-04-02 14:30:00' },
      { qrcodeId: 'Q1003', companyId: '1001', companyName: '攸县香干食品有限公司', status: 'ACTIVE', createTime: '2026-03-15 09:00:00' },
      { qrcodeId: 'Q1004', companyId: '1002', companyName: '湖南特产食品厂', status: 'FROZEN', createTime: '2026-03-20 16:00:00' }
    ];
  } catch (err) {
    console.error('加载二维码列表失败:', err);
    error.value = `加载失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function searchQrcodes() {
  loading.value = true;
  error.value = '';
  try {
    // 假设后端有二维码查询接口
    // 这里暂时使用模拟数据，实际项目中需要替换为真实接口
    // const res = await apiFetch('/qrcode/search', {
    //   method: 'POST',
    //   headers: {
    //     'Content-Type': 'application/json',
    //     ...authHeaders(token.value)
    //   },
    //   body: JSON.stringify(queryForm.value)
    // });
    // qrcodes.value = res.data || [];
    
    // 模拟数据
    qrcodes.value = [
      { qrcodeId: 'Q1001', companyId: '1001', companyName: '攸县香干食品有限公司', status: 'PENDING', createTime: '2026-04-01 10:00:00' },
      { qrcodeId: 'Q1003', companyId: '1001', companyName: '攸县香干食品有限公司', status: 'ACTIVE', createTime: '2026-03-15 09:00:00' }
    ];
  } catch (err) {
    console.error('查询二维码失败:', err);
    error.value = `查询失败: ${err.message || '未知错误'}`;
  } finally {
    loading.value = false;
  }
}

async function reviewQrcode(qrcodeId, approved) {
  loading.value = true;
  try {
    // 假设后端有二维码审核接口
    // await apiFetch(`/qrcode/review/${encodeURIComponent(qrcodeId)}`, {
    //   method: 'PUT',
    //   headers: {
    //     'Content-Type': 'application/json',
    //     ...authHeaders(token.value)
    //   },
    //   body: JSON.stringify({ approved })
    // });
    
    // 模拟审核成功
    console.log(`审核二维码 ${qrcodeId}: ${approved ? '通过' : '拒绝'}`);
    await loadPendingQrcodes();
    await loadAllQrcodes();
  } catch (error) {
    console.error('审核二维码失败:', error);
  } finally {
    loading.value = false;
  }
}

async function freezeQrcode(qrcodeId, frozen) {
  loading.value = true;
  try {
    // 假设后端有二维码冻结/解冻接口
    // await apiFetch(`/qrcode/status/${encodeURIComponent(qrcodeId)}`, {
    //   method: 'PUT',
    //   headers: {
    //     'Content-Type': 'application/json',
    //     ...authHeaders(token.value)
    //   },
    //   body: JSON.stringify({ status: frozen ? 'FROZEN' : 'ACTIVE' })
    // });
    
    // 模拟冻结/解冻成功
    console.log(`${frozen ? '冻结' : '解冻'}二维码 ${qrcodeId}`);
    await loadAllQrcodes();
  } catch (error) {
    console.error('操作二维码失败:', error);
  } finally {
    loading.value = false;
  }
}

function viewQrcodeDetail(qrcodeId) {
  // 查看二维码详情，实际项目中可以跳转到详情页面或显示弹窗
  console.log('查看二维码详情:', qrcodeId);
  // 这里可以添加详情查看逻辑
}

onMounted(async () => {
  await Promise.all([loadPendingQrcodes(), loadAllQrcodes()]);
});
</script>

<style scoped>
.qrcode-management {
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
  min-width: 150px;
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

.status-active {
  background: #dbeafe;
  color: #2563eb;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-frozen {
  background: #e5e7eb;
  color: #4b5563;
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