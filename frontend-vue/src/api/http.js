import { normalizeBearerToken } from './session';

export async function apiFetch(url, options = {}) {
  const resp = await fetch(url, options);
  const contentType = resp.headers.get('content-type') || '';
  const data = contentType.includes('application/json') ? await resp.json() : await resp.text();
  if (!resp.ok) {
    const message = (data && typeof data === 'object' ? data.msg : '') || `请求失败(${resp.status})`;
    const error = new Error(message);
    error.status = resp.status;
    error.payload = data;
    throw error;
  }
  return data;
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

