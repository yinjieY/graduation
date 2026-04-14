export function isEmpty(value) {
  return value === null || value === undefined || value === '' || 
         (Array.isArray(value) && value.length === 0) ||
         (typeof value === 'object' && Object.keys(value).length === 0);
}

export function isNotEmpty(value) {
  return !isEmpty(value);
}

export function isValidEmail(email) {
  if (!email) return false;
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
}

export function isValidPhone(phone) {
  if (!phone) return false;
  const regex = /^1[3-9]\d{9}$/;
  return regex.test(phone);
}

export function isValidIdCard(idCard) {
  if (!idCard) return false;
  const regex = /^\d{17}[\dXx]$/;
  return regex.test(idCard);
}

export function validateRequired(field, value, message) {
  if (isEmpty(value)) {
    return { valid: false, message: message || `${field}不能为空` };
  }
  return { valid: true, message: '' };
}

export function validateEmail(value) {
  if (isEmpty(value)) return { valid: true, message: '' };
  if (!isValidEmail(value)) {
    return { valid: false, message: '请输入有效的邮箱地址' };
  }
  return { valid: true, message: '' };
}

export function validatePhone(value) {
  if (isEmpty(value)) return { valid: true, message: '' };
  if (!isValidPhone(value)) {
    return { valid: false, message: '请输入有效的手机号码' };
  }
  return { valid: true, message: '' };
}