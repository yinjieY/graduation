import { apiFetch, authHeaders } from './http';

// 批次管理
export function createProductBatch(payload, token) {
  // 从localStorage获取companyUserInfo
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      payload.companyId = companyId;
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/trace/batch/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getProductBatchList(token) {
  // 从localStorage获取companyUserInfo
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/trace/batch/list?companyId=${encodeURIComponent(companyId)}`, { headers: authHeaders(token) });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/trace/batch/list', { headers: authHeaders(token) });
}

export function updateProductBatch(payload, token) {
  // 从localStorage获取companyUserInfo
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      payload.companyId = companyId;
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/trace/batch/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function deleteProductBatch(batchId, token) {
  // 从localStorage获取companyUserInfo
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/trace/batch/delete/${encodeURIComponent(batchId)}?companyId=${encodeURIComponent(companyId)}`, {
        method: 'DELETE',
        headers: authHeaders(token)
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch(`/trace/batch/delete/${encodeURIComponent(batchId)}`, {
    method: 'DELETE',
    headers: authHeaders(token)
  });
}

export function applyForReview(batchId, token) {
  return apiFetch(`/trace/batch/${encodeURIComponent(batchId)}/apply-review`, {
    method: 'POST',
    headers: authHeaders(token)
  });
}

export function getPendingBatches(token) {
  return apiFetch('/trace/batch/pending', {
    headers: authHeaders(token)
  }).then(response => {
    // 确保返回的数据格式正确
    if (response && response.data) {
      return response;
    }
    return { data: [] };
  });
}

export function reviewBatch(batchId, status, comment, token) {
  return apiFetch(`/trace/batch/${encodeURIComponent(batchId)}/review?status=${encodeURIComponent(status)}&comment=${encodeURIComponent(comment || '')}`, {
    method: 'PUT',
    headers: authHeaders(token)
  });
}

// 溯源码管理
export function generateQrCodes(batchId, count, token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    const payload = { batchId, maxAllowedScans: count };
    if (companyId) {
      payload.companyId = companyId;
    }
    return apiFetch('/trace/qs/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders(token)
      },
      body: JSON.stringify(payload)
    });
  } catch (error) {
    console.error('获取companyId失败:', error);
    return apiFetch('/trace/qs/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...authHeaders(token)
      },
      body: JSON.stringify({ batchId, maxAllowedScans: count })
    });
  }
}

export function getQrCodeList(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/trace/qs/list?companyId=${encodeURIComponent(companyId)}`, { headers: authHeaders(token), noCache: true });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/trace/qs/list', { headers: authHeaders(token), noCache: true });
}

export function updateQrCodeStatus(qrCodeId, status, token) {
  return apiFetch(`/trace/qs/${encodeURIComponent(qrCodeId)}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ status })
  });
}

export function getQrCodeDetail(qrCodeId, token) {
  return apiFetch(`/trace/qs/get/${encodeURIComponent(qrCodeId)}`, { headers: authHeaders(token) });
}

// 企业信息管理
export function getCompanyInfo(token) {
  return apiFetch('/trace/company/info', { headers: authHeaders(token) });
}

export function updateCompanyInfo(payload, token) {
  return apiFetch('/trace/company/info', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getCompanyById(companyId, token) {
  return apiFetch(`/trace/company/${encodeURIComponent(companyId)}`, { headers: authHeaders(token) });
}

// 企业管理
export function createCompany(payload, token) {
  return apiFetch('/trace/company/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function updateCompany(payload, token) {
  return apiFetch('/trace/company/update', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getCompanyList(token) {
  return apiFetch('/trace/company/list', {
    headers: authHeaders(token)
  });
}

// 溯源信息查询
export function getTraceDetail(qsId) {
  return apiFetch(`/trace/query/${encodeURIComponent(qsId)}`);
}