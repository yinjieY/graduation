import { apiFetch, authHeaders } from './http';

// 区块链存证管理
export function getProofList(businessKey, token) {
  return apiFetch(`/block/proof/list/${encodeURIComponent(businessKey)}`, { headers: authHeaders(token) });
}

export function verifyProofHash(businessKey, hash, token) {
  const query = new URLSearchParams({ businessKey, hash }).toString();
  return apiFetch(`/block/proof/verify?${query}`, { headers: authHeaders(token) });
}