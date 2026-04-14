export * from './statusCodes';
export * from './riskLevels';

export const API_BASE_URL = 'http://localhost:9090';

export const STORAGE_KEYS = {
  ADMIN_TOKEN: 'admin_token',
  COMPANY_TOKEN: 'company_token',
  COMPANY_USER_INFO: 'companyUserInfo',
  ADMIN_USER_INFO: 'adminUserInfo'
};

export const DATE_FORMATS = {
  DATE: 'yyyy-MM-dd',
  DATETIME: 'yyyy-MM-dd HH:mm:ss',
  DISPLAY: 'YYYY/MM/DD HH:mm:ss'
};

export const PAGINATION = {
  DEFAULT_SIZE: 20,
  MAX_SIZE: 500
};