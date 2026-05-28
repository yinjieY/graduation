import { apiFetch, authHeaders } from './http';

export function getMessages(token) {
  console.log('调用 getMessages，token:', token);
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    console.log('companyId:', companyId);
    if (companyId) {
      return apiFetch(`/alert/messages?companyId=${encodeURIComponent(companyId)}`, {
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/messages', {
    headers: authHeaders(token),
    noCache: true
  });
}

export function getSystemNotifications(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/system-notifications?companyId=${encodeURIComponent(companyId)}`, {
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/system-notifications', {
    headers: authHeaders(token),
    noCache: true
  });
}

export function getUnreadNotificationCount(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/system-notifications/unread/count?companyId=${encodeURIComponent(companyId)}`, {
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/system-notifications/unread/count', {
    headers: authHeaders(token),
    noCache: true
  });
}

export function markNotificationAsRead(notificationId, token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/system-notifications/${encodeURIComponent(notificationId)}/read?companyId=${encodeURIComponent(companyId)}`, {
        method: 'POST',
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch(`/alert/system-notifications/${encodeURIComponent(notificationId)}/read`, {
    method: 'POST',
    headers: authHeaders(token),
    noCache: true
  });
}

export function markAllNotificationsAsRead(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/system-notifications/read/all?companyId=${encodeURIComponent(companyId)}`, {
        method: 'POST',
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/system-notifications/read/all', {
    method: 'POST',
    headers: authHeaders(token),
    noCache: true
  });
}

// 预警管理
export function getAlertList(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/alert/list?${query}`, { 
    headers: authHeaders(token),
    noCache: true
  });
}

export function getUnreadAlertCount(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/unread/count?companyId=${encodeURIComponent(companyId)}`, {
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/unread/count', {
    headers: authHeaders(token),
    noCache: true
  });
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

export function updateRuleWeight(ruleId, scoreWeight, token) {
  return apiFetch(`/alert/rules/${encodeURIComponent(ruleId)}/weight`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ scoreWeight })
  });
}

export function reloadRules(token) {
  return apiFetch('/alert/rules/reload', {
    method: 'POST',
    headers: authHeaders(token)
  });
}

export function markAlertAsRead(alertId, token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/${encodeURIComponent(alertId)}/read?companyId=${encodeURIComponent(companyId)}`, {
        method: 'POST',
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch(`/alert/${encodeURIComponent(alertId)}/read`, {
    method: 'POST',
    headers: authHeaders(token),
    noCache: true
  });
}

export function markAllAlertsAsRead(token) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('companyUserInfo') || '{}');
    const companyId = userInfo.companyId || '';
    if (companyId) {
      return apiFetch(`/alert/read/all?companyId=${encodeURIComponent(companyId)}`, {
        method: 'POST',
        headers: authHeaders(token),
        noCache: true
      });
    }
  } catch (error) {
    console.error('获取companyId失败:', error);
  }
  return apiFetch('/alert/read/all', {
    method: 'POST',
    headers: authHeaders(token),
    noCache: true
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

