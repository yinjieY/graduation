import { apiFetch, authHeaders } from './http';

export function getMessages(token) {
  return apiFetch('/alert/messages', { headers: authHeaders(token) });
}

// 预警管理
export function getAlertList(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/alert/list?${query}`, { headers: authHeaders(token) });
}

// 规则管理
export function getAlertRules(token) {
  return apiFetch('/alert/rules', { headers: authHeaders(token) });
}

export function updateRuleThreshold(ruleId, threshold, token) {
  return apiFetch(`/alert/rules/${encodeURIComponent(ruleId)}/threshold`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ threshold })
  });
}

export function updateRuleStatus(ruleId, status, token) {
  return apiFetch(`/alert/rules/${encodeURIComponent(ruleId)}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ status })
  });
}

export function reloadRules(token) {
  return apiFetch('/alert/rules/reload', {
    method: 'POST',
    headers: authHeaders(token)
  });
}

// 风险评估
export function evaluateRisk(payload, token) {
  return apiFetch('/alert/evaluate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

