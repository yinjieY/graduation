export function extractData(response) {
  if (!response || typeof response !== 'object') {
    return null;
  }
  return response.data;
}

export function extractList(response) {
  const data = extractData(response);
  if (!data) return [];
  return Array.isArray(data) ? data : (data.list || []);
}

export function extractPageData(response) {
  const data = extractData(response);
  if (!data) {
    return { list: [], total: 0, page: 1, size: 20 };
  }
  return {
    list: data.list || [],
    total: data.total || 0,
    page: data.page || 1,
    size: data.size || 20
  };
}

export function isSuccess(response) {
  return response && response.code === 200;
}

export function getMessage(response) {
  return response && response.msg || '';
}

export function handleApiError(error) {
  if (error && error.message) {
    return error.message;
  }
  if (error && error.payload && error.payload.msg) {
    return error.payload.msg;
  }
  return '网络请求失败，请稍后重试';
}