<template>
  <Layout role="admin">
    <div class="rule-management">
      <div class="page-header">
        <h1 class="page-title">预警规则管理</h1>
        <p class="page-subtitle">配置风险评估规则参数，调整阈值与权重</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>规则列表</h3>
          <div class="header-actions">
            <button class="btn-refresh" :disabled="loading" @click="loadRules">
              <span v-if="loading" class="loading-spinner"></span>
              <span v-else>⟳</span>
              刷新
            </button>
            <button class="btn-primary" :disabled="loading" @click="handleReload">
              热加载规则
            </button>
          </div>
        </div>

        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>规则ID</th>
                <th>规则名称</th>
                <th>触发阈值</th>
                <th>分数权重</th>
                <th>预警等级</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="rules.length === 0">
                <td colspan="7" class="empty-state">暂无规则数据</td>
              </tr>
              <tr v-for="rule in rules" :key="rule.ruleId">
                <td class="rule-id">{{ rule.ruleId }}</td>
                <td class="rule-name">{{ rule.ruleName }}</td>
                <td>
                  <div class="editable-cell">
                    <span v-if="editingRule !== rule.ruleId + '_threshold'" class="cell-value" @dblclick="startEdit(rule, 'threshold')">
                      {{ rule.threshold }}
                    </span>
                    <div v-else class="cell-edit">
                      <input v-model="editForm.threshold" class="edit-input" @keyup.enter="saveThreshold(rule)" @keyup.escape="cancelEdit" ref="thresholdInput" />
                      <button class="btn-save" @click="saveThreshold(rule)">✓</button>
                      <button class="btn-cancel-sm" @click="cancelEdit">✕</button>
                    </div>
                  </div>
                </td>
                <td>
                  <div class="editable-cell">
                    <span v-if="editingRule !== rule.ruleId + '_weight'" class="cell-value" @dblclick="startEdit(rule, 'weight')">
                      {{ rule.scoreWeight }}
                    </span>
                    <div v-else class="cell-edit">
                      <input v-model="editForm.scoreWeight" type="number" step="0.1" min="0" max="1" class="edit-input" @keyup.enter="saveWeight(rule)" @keyup.escape="cancelEdit" />
                      <button class="btn-save" @click="saveWeight(rule)">✓</button>
                      <button class="btn-cancel-sm" @click="cancelEdit">✕</button>
                    </div>
                  </div>
                </td>
                <td>
                  <span :class="['level-badge', `level-${rule.alertLevel}`]">
                    {{ getLevelText(rule.alertLevel) }}
                  </span>
                </td>
                <td>
                  <span :class="['status-badge', rule.status === 1 ? 'status-on' : 'status-off']">
                    {{ rule.status === 1 ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="actions">
                  <button
                    :class="rule.status === 1 ? 'btn-disable' : 'btn-enable'"
                    @click="toggleStatus(rule)"
                    :disabled="loading"
                  >
                    {{ rule.status === 1 ? '禁用' : '启用' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="table-footer">
          <span class="hint-text">双击阈值或权重可直接编辑，修改后按回车或点击✓保存</span>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref, nextTick } from 'vue';
import { getAlertRules, updateRuleThreshold, updateRuleWeight, updateRuleStatus, reloadRules } from '../../api/alert';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const rules = ref([]);
const token = ref(getToken('admin'));
const editingRule = ref('');
const editForm = ref({ threshold: '', scoreWeight: '' });
const thresholdInput = ref(null);

function getLevelText(level) {
  switch (level) {
    case 0: return '严重';
    case 1: return '高风险';
    case 2: return '中等';
    default: return level;
  }
}

function startEdit(rule, field) {
  if (field === 'threshold') {
    editingRule.value = rule.ruleId + '_threshold';
    editForm.value.threshold = rule.threshold;
  } else if (field === 'weight') {
    editingRule.value = rule.ruleId + '_weight';
    editForm.value.scoreWeight = rule.scoreWeight;
  }
}

function cancelEdit() {
  editingRule.value = '';
}

async function saveThreshold(rule) {
  loading.value = true;
  try {
    await updateRuleThreshold(rule.ruleId, editForm.value.threshold, token.value);
    rule.threshold = editForm.value.threshold;
    editingRule.value = '';
  } catch (err) {
    console.error('更新阈值失败:', err);
  } finally {
    loading.value = false;
  }
}

async function saveWeight(rule) {
  const weight = parseFloat(editForm.value.scoreWeight);
  if (isNaN(weight) || weight < 0 || weight > 1) {
    alert('权重必须在0到1之间');
    return;
  }
  loading.value = true;
  try {
    await updateRuleWeight(rule.ruleId, weight, token.value);
    rule.scoreWeight = weight;
    editingRule.value = '';
  } catch (err) {
    console.error('更新权重失败:', err);
  } finally {
    loading.value = false;
  }
}

async function toggleStatus(rule) {
  loading.value = true;
  try {
    const newStatus = rule.status === 1 ? 0 : 1;
    await updateRuleStatus(rule.ruleId, newStatus, token.value);
    rule.status = newStatus;
  } catch (err) {
    console.error('更新状态失败:', err);
  } finally {
    loading.value = false;
  }
}

async function handleReload() {
  loading.value = true;
  try {
    await reloadRules(token.value);
    await loadRules();
  } catch (err) {
    console.error('热加载失败:', err);
  } finally {
    loading.value = false;
  }
}

async function loadRules() {
  loading.value = true;
  try {
    const res = await getAlertRules(token.value);
    rules.value = res.data || [];
  } catch (err) {
    console.error('加载规则失败:', err);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadRules();
});
</script>

<style scoped>
.rule-management {
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

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
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

.btn-primary {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  background: #3b82f6;
  color: white;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-primary:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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

.rule-id {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #1e293b;
}

.rule-name {
  font-weight: 500;
  color: #1e293b;
}

.editable-cell .cell-value {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.editable-cell .cell-value:hover {
  background: #f1f5f9;
}

.cell-edit {
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-input {
  width: 100px;
  padding: 4px 8px;
  border: 2px solid #3b82f6;
  border-radius: 4px;
  font-size: 14px;
  outline: none;
}

.btn-save {
  background: #10b981;
  color: white;
  border: none;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-cancel-sm {
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.level-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.level-0 {
  background: #fef2f2;
  color: #dc2626;
}

.level-1 {
  background: #fff7ed;
  color: #ea580c;
}

.level-2 {
  background: #eff6ff;
  color: #2563eb;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.status-on {
  background: #d1fae5;
  color: #059669;
}

.status-off {
  background: #f1f5f9;
  color: #94a3b8;
}

.actions {
  display: flex;
  gap: 8px;
}

.btn-enable {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: #10b981;
  color: white;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-enable:hover:not(:disabled) {
  background: #059669;
}

.btn-disable {
  padding: 6px 12px;
  border: 1px solid #fecaca;
  border-radius: 6px;
  background: white;
  color: #dc2626;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-disable:hover:not(:disabled) {
  background: #fef2f2;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top: 2px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  display: inline-block;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.table-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}

.hint-text {
  font-size: 13px;
  color: #94a3b8;
}
</style>
