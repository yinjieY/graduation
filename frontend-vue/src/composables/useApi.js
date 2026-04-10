import { ref } from 'vue';
import * as api from '../api';

export function useApi() {
  const wrapped = {};
  Object.keys(api).forEach((name) => {
    const fn = api[name];
    if (typeof fn !== 'function') {
      wrapped[name] = fn;
      return;
    }
    wrapped[name] = async (...args) => {
      const res = await fn(...args);
      // Unwrap common R<T> payload to simplify view-layer data handling.
      if (res && typeof res === 'object' && Object.prototype.hasOwnProperty.call(res, 'code')) {
        if (res.code !== 200) {
          const error = new Error(res.msg || '请求失败');
          error.payload = res;
          throw error;
        }
        return Object.prototype.hasOwnProperty.call(res, 'data') ? res.data : res;
      }
      return res;
    };
  });
  return wrapped;
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