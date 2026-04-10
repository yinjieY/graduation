import { apiFetch, authHeaders } from './http';

export function login(account, password) {
  const body = new URLSearchParams({ account, password });
  return apiFetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString()
  });
}

export function registerAdmin(payload) {
  return apiFetch('/auth/register/admin', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export function registerCompany(payload) {
  return apiFetch('/auth/register/user', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

export function getPending(token) {
  return apiFetch('/auth/company/pending', {
    headers: authHeaders(token),
    noCache: true
  });
}

export function reviewCompany(companyId, approved, token) {
  return apiFetch(`/auth/company/review/${encodeURIComponent(companyId)}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify({ approved, remark: approved ? '管理员审核通过' : '管理员审核拒绝' })
  });
}

export function submitCompanyApply(payload, token) {
  return apiFetch('/auth/company/apply', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token)
    },
    body: JSON.stringify(payload)
  });
}

export function queryCompanyStatus(companyId, token) {
  return apiFetch(`/auth/company/status/${encodeURIComponent(companyId)}`, {
    headers: authHeaders(token)
  });
}

