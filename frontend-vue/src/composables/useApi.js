import { ref } from 'vue';
import * as api from '../api';

export function useApi() {
  return api;
}

// 通用错误处理
export function handleApiError(error) {
  console.error('API Error:', error);
  const message = error.message || '请求失败，请稍后重试';
  // 这里可以添加全局错误处理，例如显示错误提示
  return message;
}

// 通用加载状态管理
export function useLoading() {
  const loading = ref(false);
  
  const withLoading = async (fn) => {
    loading.value = true;
    try {
      return await fn();
    } finally {
      loading.value = false;
    }
  };
  
  return {
    loading,
    withLoading
  };
}