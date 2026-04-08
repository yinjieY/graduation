import { apiFetch, authHeaders } from './http';

export function getFeedbackList(token) {
  return apiFetch('/scan/feedback/list', { headers: authHeaders(token) });
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

