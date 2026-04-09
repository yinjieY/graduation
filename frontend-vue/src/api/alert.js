import { apiFetch, authHeaders } from './http';

export function getMessages(token) {
  return apiFetch('/alert/messages', { headers: authHeaders(token) });
}

// 预警管理
export function getAlertList(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/alert/list?${query}`, { headers: authHeaders(token) });
}

export function getAlertDetail(alertId, token) {
  return apiFetch(`/alert/detail/${encodeURIComponent(alertId)}`, { headers: authHeaders(token) });
}

export function submitAlertAppeal(alertId, payload, token) {
  return apiFetch(`/alert/appeal/${encodeURIComponent(alertId)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function getAlertStats(token) {
  return apiFetch('/alert/stats', { headers: authHeaders(token) });
}

// 管理员预警审批
export function reviewAlert(alertId, approved, token) {
  return apiFetch(`/alert/review/${encodeURIComponent(alertId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ approved })
  });
}

export function getGlobalAlertList(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/alert/admin/list?${query}`, { headers: authHeaders(token) });
}

