export function formatDate(dateStr, format = 'YYYY/MM/DD HH:mm:ss') {
  if (!dateStr) return '-';
  try {
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '-';
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    
    return format
      .replace('YYYY', year)
      .replace('MM', month)
      .replace('DD', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds);
  } catch {
    return '-';
  }
}

export function formatNumber(num, decimals = 0) {
  if (num === null || num === undefined || isNaN(num)) return '0';
  return Number(num).toFixed(decimals);
}

export function formatPercent(value) {
  if (value === null || value === undefined || isNaN(value)) return '0%';
  return `${Number(value).toFixed(0)}%`;
}

export function truncateText(text, maxLength = 50) {
  if (!text) return '-';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
}