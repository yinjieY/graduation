import { normalizeBearerToken } from './session';

// 请求缓存
const cache = new Map();
const CACHE_DURATION = 5 * 60 * 1000; // 5分钟缓存

// 请求队列，用于防止重复请求
const pendingRequests = new Map();

// 构建请求缓存键
function getCacheKey(url, options) {
  return `${url}_${JSON.stringify(options)}`;
}

// 带超时的fetch
function fetchWithTimeout(url, options, timeout = 30000) {
  return Promise.race([
    fetch(url, options),
    new Promise((_, reject) => 
      setTimeout(() => reject(new Error('请求超时')), timeout)
    )
  ]);
}

// 带重试的fetch
async function fetchWithRetry(url, options, retries = 3, delay = 1000) {
  for (let i = 0; i < retries; i++) {
    try {
      return await fetchWithTimeout(url, options);
    } catch (error) {
      if (i === retries - 1) throw error;
      // 只对网络错误进行重试
      if (!error.message.includes('网络') && !error.message.includes('timeout')) throw error;
      await new Promise(resolve => setTimeout(resolve, delay * (i + 1)));
    }
  }
}

export async function apiFetch(url, options = {}) {
  // 构建缓存键
  const cacheKey = getCacheKey(url, options);
  
  // 检查是否有缓存且未过期
  const cached = cache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
    return cached.data;
  }
  
  // 检查是否有相同请求正在进行
  if (pendingRequests.has(cacheKey)) {
    return pendingRequests.get(cacheKey);
  }
  
  // 创建请求Promise
  const requestPromise = (async () => {
    try {
      const resp = await fetchWithRetry(url, options);
      const contentType = resp.headers.get('content-type') || '';
      const data = contentType.includes('application/json') ? await resp.json() : await resp.text();
      
      if (!resp.ok) {
        const message = (data && typeof data === 'object' ? data.msg : '') || `请求失败(${resp.status})`;
        const error = new Error(message);
        error.status = resp.status;
        error.payload = data;
        throw error;
      }
      
      // 缓存成功的响应
      cache.set(cacheKey, {
        data,
        timestamp: Date.now()
      });
      
      return data;
    } finally {
      // 移除请求队列
      pendingRequests.delete(cacheKey);
    }
  })();
  
  // 添加到请求队列
  pendingRequests.set(cacheKey, requestPromise);
  
  return requestPromise;
}

export function authHeaders(token) {
  const bearer = normalizeBearerToken(token);
  if (!bearer) {
    return {};
  }
  return {
    Authorization: bearer
  };
}

// 清除缓存
export function clearCache() {
  cache.clear();
}

// 清除特定缓存
export function clearCacheByUrl(url) {
  for (const key of cache.keys()) {
    if (key.startsWith(url)) {
      cache.delete(key);
    }
  }
}

