export async function apiFetch(url, options = {}) {
  const resp = await fetch(url, options);
  const data = await resp.json();
  if (!resp.ok) {
    throw new Error(data?.msg || '请求失败');
  }
  return data;
}

export function authHeaders(token) {
  if (!token) {
    return {};
  }
  return {
    Authorization: token.startsWith('Bearer ') ? token : `Bearer ${token}`
  };
}

