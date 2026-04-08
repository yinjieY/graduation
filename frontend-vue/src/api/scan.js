import { apiFetch } from './http';

export function queryTrace(qsId) {
  return apiFetch(`/trace/query/${encodeURIComponent(qsId)}`);
}

export function reportScan(payload) {
  return apiFetch('/scan/report', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
}

