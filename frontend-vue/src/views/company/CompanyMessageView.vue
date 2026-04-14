<template>
  <Layout role="company">
    <div class="message-view">
      <div class="page-header">
        <h1 class="page-title">系统消息</h1>
        <p class="page-subtitle">查看系统公告、审核结果和操作通知</p>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>通知列表</h3>
          <div class="card-actions">
            <button class="btn-mark-all" :disabled="loading" @click="markAllAsRead">
              全部已读
            </button>
            <button class="btn-refresh" :disabled="loading" @click="loadNotifications">
              <span v-if="loading" class="loading-spinner"></span>
              刷新
            </button>
          </div>
        </div>
        <div class="notification-list">
          <div v-if="notifications.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <p>暂无系统通知</p>
          </div>
          <div 
            v-for="item in notifications" 
            :key="item.notificationId"
            :class="['notification-item', { 'is-unread': item.status === 'UNREAD' }]"
          >
            <div class="notification-icon">
              <span v-if="item.type === 'SYSTEM'">📢</span>
              <span v-else-if="item.type === 'REVIEW'">✅</span>
              <span v-else>📝</span>
            </div>
            <div class="notification-content">
              <div class="notification-header">
                <span class="notification-title">{{ item.title }}</span>
                <span :class="['notification-type', `type-${item.type.toLowerCase()}`]">
                  {{ getTypeLabel(item.type) }}
                </span>
              </div>
              <p class="notification-body">{{ item.content }}</p>
              <div class="notification-footer">
                <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
                <span v-if="item.status === 'UNREAD'" class="unread-badge">未读</span>
                <button class="btn-view-detail" @click.stop="viewDetail(item)">
                  查看详情
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="showModal" class="modal-overlay" @click="closeModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>{{ selectedNotification?.title }}</h3>
            <button class="modal-close" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <div class="detail-row">
              <span class="detail-label">通知类型：</span>
              <span :class="['detail-value', `type-${selectedNotification?.type?.toLowerCase()}`]">
                {{ getTypeLabel(selectedNotification?.type) }}
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">通知内容：</span>
              <span class="detail-value">{{ selectedNotification?.content }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">来源模块：</span>
              <span class="detail-value">{{ selectedNotification?.sourceModule || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">业务ID：</span>
              <span class="detail-value">{{ selectedNotification?.sourceId || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">创建时间：</span>
              <span class="detail-value">{{ formatDateTime(selectedNotification?.createdAt) }}</span>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-close-modal" @click="closeModal">关闭</button>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getSystemNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '../../api/alert';
import { getToken } from '../../api/session';
import Layout from '../../components/Layout.vue';

const loading = ref(false);
const notifications = ref([]);
const token = ref(getToken('company'));
const showModal = ref(false);
const selectedNotification = ref(null);

const typeLabels = {
  SYSTEM: '系统公告',
  REVIEW: '审核结果',
  OPERATION: '操作日志'
};

function getTypeLabel(type) {
  return typeLabels[type] || type;
}

function formatTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor(diff / (1000 * 60));
  
  if (days > 0) return `${days}天前`;
  if (hours > 0) return `${hours}小时前`;
  if (minutes > 0) return `${minutes}分钟前`;
  return '刚刚';
}

function formatDateTime(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

async function loadNotifications() {
  loading.value = true;
  try {
    const res = await getSystemNotifications(token.value);
    notifications.value = res.data || [];
  } catch (error) {
    console.error('加载系统通知失败:', error);
  } finally {
    loading.value = false;
  }
}

async function markAsRead(item) {
  if (item.status === 'READ') return;
  try {
    await markNotificationAsRead(item.notificationId, token.value);
    item.status = 'READ';
  } catch (error) {
    console.error('标记已读失败:', error);
  }
}

async function markAllAsRead() {
  loading.value = true;
  try {
    await markAllNotificationsAsRead(token.value);
    notifications.value.forEach(item => {
      item.status = 'READ';
    });
  } catch (error) {
    console.error('全部标记已读失败:', error);
  } finally {
    loading.value = false;
  }
}

async function viewDetail(item) {
  selectedNotification.value = item;
  showModal.value = true;
  await markAsRead(item);
}

function closeModal() {
  showModal.value = false;
  selectedNotification.value = null;
}

onMounted(async () => {
  await loadNotifications();
});
</script>

<style scoped>
.message-view {
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

.card-actions {
  display: flex;
  gap: 12px;
}

.btn-refresh,
.btn-mark-all {
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
  font-size: 14px;
}

.btn-refresh:hover:not(:disabled),
.btn-mark-all:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.btn-refresh:disabled,
.btn-mark-all:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-mark-all {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.btn-mark-all:hover:not(:disabled) {
  background: #2563eb;
  border-color: #2563eb;
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

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  font-style: italic;
  margin: 0;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 4px solid #e2e8f0;
}

.notification-item:hover {
  background: #f1f5f9;
}

.notification-item.is-unread {
  background: white;
  border-left-color: #3b82f6;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.notification-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.notification-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.notification-type {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.notification-type.type-system {
  background: #dbeafe;
  color: #1d4ed8;
}

.notification-type.type-review {
  background: #dcfce7;
  color: #166534;
}

.notification-type.type-operation {
  background: #fef3c7;
  color: #92400e;
}

.notification-body {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 8px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

.unread-badge {
  background: #ef4444;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.btn-view-detail {
  padding: 4px 12px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-left: auto;
}

.btn-view-detail:hover {
  background: #2563eb;
}

.modal-overlay {
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

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #64748b;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: #1e293b;
}

.modal-body {
  padding: 20px;
  max-height: 400px;
  overflow-y: auto;
}

.detail-row {
  display: flex;
  margin-bottom: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
}

.detail-row:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.detail-label {
  font-weight: 500;
  color: #64748b;
  min-width: 80px;
  flex-shrink: 0;
}

.detail-value {
  color: #1e293b;
  word-break: break-all;
}

.detail-value.type-system {
  color: #1d4ed8;
}

.detail-value.type-review {
  color: #166534;
}

.detail-value.type-operation {
  color: #92400e;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: flex-end;
}

.btn-close-modal {
  padding: 8px 20px;
  background: #3b82f6;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.btn-close-modal:hover {
  background: #2563eb;
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
  
  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .notification-item {
    padding: 12px;
  }
}
</style>