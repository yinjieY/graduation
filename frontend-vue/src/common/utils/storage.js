export function getStorage(key, defaultValue = null) {
  try {
    const value = localStorage.getItem(key);
    if (value === null || value === 'undefined') {
      return defaultValue;
    }
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  } catch {
    return defaultValue;
  }
}

export function setStorage(key, value) {
  try {
    const serialized = typeof value === 'string' ? value : JSON.stringify(value);
    localStorage.setItem(key, serialized);
    return true;
  } catch {
    return false;
  }
}

export function removeStorage(key) {
  try {
    localStorage.removeItem(key);
    return true;
  } catch {
    return false;
  }
}

export function clearStorage() {
  try {
    localStorage.clear();
    return true;
  } catch {
    return false;
  }
}

export function getSessionStorage(key, defaultValue = null) {
  try {
    const value = sessionStorage.getItem(key);
    if (value === null || value === 'undefined') {
      return defaultValue;
    }
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  } catch {
    return defaultValue;
  }
}

export function setSessionStorage(key, value) {
  try {
    const serialized = typeof value === 'string' ? value : JSON.stringify(value);
    sessionStorage.setItem(key, serialized);
    return true;
  } catch {
    return false;
  }
}