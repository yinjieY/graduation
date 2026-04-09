import { apiFetch, authHeaders } from './http';

// 区块链存证管理
export function getProofList(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/block/proof/list?${query}`, { headers: authHeaders(token) });
}

export function getProofDetail(proofId, token) {
  return apiFetch(`/block/proof/${encodeURIComponent(proofId)}`, { headers: authHeaders(token) });
}

export function verifyProofHash(hash, token) {
  return apiFetch(`/block/proof/verify/${encodeURIComponent(hash)}`, { headers: authHeaders(token) });
}

export function exportProofData(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/block/proof/export?${query}`, {
    headers: {
      ...authHeaders(token),
      'Accept': 'application/octet-stream'
    }
  });
}

// 存证统计
export function getProofStats(token) {
  return apiFetch('/block/proof/stats', { headers: authHeaders(token) });
}

// 上链记录查询
export function getChainRecords(params, token) {
  const query = new URLSearchParams(params).toString();
  return apiFetch(`/block/chain/records?${query}`, { headers: authHeaders(token) });
}