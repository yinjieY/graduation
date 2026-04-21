import { ref } from 'vue';

// 单例模式 - 创建全局共享的通知状态
const notifications = ref([]);
let notificationId = 0;

const showNotification = (message, type = 'info', duration = 3000) => {
  const id = ++notificationId;
  notifications.value.push({
    id,
    message,
    type,
    duration
  });

  return id;
};

const showSuccess = (message, duration) => {
  return showNotification(message, 'success', duration);
};

const showError = (message, duration) => {
  return showNotification(message, 'error', duration);
};

const showWarning = (message, duration) => {
  return showNotification(message, 'warning', duration);
};

const showInfo = (message, duration) => {
  return showNotification(message, 'info', duration);
};

const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

export function useNotification() {
  return {
    notifications,
    showNotification,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeNotification
  };
}

// 导出全局通知服务，供 composable 使用
export const notificationService = {
  showNotification,
  showSuccess,
  showError,
  showWarning,
  showInfo,
  removeNotification,
  notifications
};