import { apiFetch, authHeaders } from './http';

export function getMessages(token) {
  return apiFetch('/alert/messages', { headers: authHeaders(token) });
}

