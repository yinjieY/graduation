const tokenKeyMap = {
  admin: 'adminToken',
  company: 'companyToken'
};

export function normalizeBearerToken(token) {
  if (!token) return '';
  const trimmed = String(token).trim();
  if (!trimmed) return '';
  return trimmed.startsWith('Bearer ') ? trimmed : `Bearer ${trimmed}`;
}

export function getToken(role) {
  const key = tokenKeyMap[role];
  if (!key) return '';
  return normalizeBearerToken(localStorage.getItem(key) || '');
}

export function setToken(role, rawToken) {
  const key = tokenKeyMap[role];
  if (!key) return '';
  const token = normalizeBearerToken(rawToken);
  if (token) {
    localStorage.setItem(key, token);
  }
  return token;
}

export function clearToken(role) {
  const key = tokenKeyMap[role];
  if (!key) return;
  localStorage.removeItem(key);
}

export function clearAllTokens() {
  Object.keys(tokenKeyMap).forEach((role) => clearToken(role));
}

