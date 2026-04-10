import { apiFetch, authHeaders } from './http';

export function getFeedbackList(token) {
  console.log('调用 getFeedbackList，token:', token);
  // 从localStorage获取companyUserInfo
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    console.log('companyId:', companyId);
    if (companyId) {
      return apiFetch(`/scan/feedback/list?companyId=${encodeURIComponent(companyId)}`, {
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/scan/feedback/list', {
    headers: authHeaders(token),
    noCache: true
  });
}

export function getFeedbackDetail(feedbackId, token) {
  return apiFetch(`/scan/feedback/detail/${encodeURIComponent(feedbackId)}`, {
    headers: authHeaders(token)
  });
}

export function updateFeedbackStatus(feedbackId, status, handleNote, token) {
  return apiFetch(`/scan/feedback/status/${encodeURIComponent(feedbackId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ status, handleNote })
  });
}

export function submitFeedback(formData) {
  return apiFetch('/scan/feedback/submit', {
    method: 'POST',
    body: formData
  });
}

export function queryFeedbackStatus(feedbackId) {
  return apiFetch(`/scan/feedback/status/${encodeURIComponent(feedbackId)}`);
}

