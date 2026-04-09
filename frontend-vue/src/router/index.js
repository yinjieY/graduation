import { createRouter, createWebHashHistory } from 'vue-router';
import { getToken } from '../api/session';

const routes = [
  { path: '/', redirect: '/scan' },
  { 
    path: '/scan', 
    component: () => import('../views/ScanView.vue'),
    meta: { title: '扫码溯源' }
  },
  
  // Admin routes
  { 
    path: '/admin/login', 
    component: () => import('../views/AdminLoginView.vue'),
    meta: { title: '管理员登录' }
  },
  { 
    path: '/admin/register', 
    component: () => import('../views/AdminRegisterView.vue'),
    meta: { title: '管理员注册' }
  },
  { 
    path: '/admin/dashboard', 
    component: () => import('../views/AdminView.vue'), 
    meta: { authRole: 'admin', title: '管理员控制台' }
  },
  {
    path: '/admin/enterprise',
    component: () => import('../views/admin/AdminEnterpriseView.vue'),
    meta: { authRole: 'admin', title: '企业管理' }
  },
  {
    path: '/admin/qrcode',
    component: () => import('../views/admin/AdminQrcodeView.vue'),
    meta: { authRole: 'admin', title: '二维码管理' }
  },
  {
    path: '/admin/feedback', 
    component: () => import('../views/admin/AdminFeedbackView.vue'), 
    meta: { authRole: 'admin', title: '反馈管理' }
  },
  { 
    path: '/admin/message', 
    component: () => import('../views/admin/AdminMessageView.vue'), 
    meta: { authRole: 'admin', title: '消息管理' }
  },
  
  // Company routes
  { 
    path: '/company/login', 
    component: () => import('../views/CompanyLoginView.vue'),
    meta: { title: '商家登录' }
  },
  { 
    path: '/company/register', 
    component: () => import('../views/CompanyRegisterView.vue'),
    meta: { title: '商家注册' }
  },
  { 
    path: '/company/dashboard', 
    component: () => import('../views/CompanyView.vue'), 
    meta: { authRole: 'company', title: '商家工作台' }
  },
  { 
    path: '/company/auth', 
    component: () => import('../views/company/CompanyAuthView.vue'), 
    meta: { authRole: 'company', title: '认证管理' }
  },
  { 
    path: '/company/feedback', 
    component: () => import('../views/company/CompanyFeedbackView.vue'), 
    meta: { authRole: 'company', title: '反馈管理' }
  },
  {
    path: '/company/message', 
    component: () => import('../views/company/CompanyMessageView.vue'), 
    meta: { authRole: 'company', title: '消息管理' }
  },
  // 新增商家功能路由
  {
    path: '/company/info',
    component: () => import('../views/company/CompanyInfoView.vue'),
    meta: { authRole: 'company', title: '企业信息管理' }
  },
  {
    path: '/company/batch',
    component: () => import('../views/company/CompanyBatchView.vue'),
    meta: { authRole: 'company', title: '生产批次管理' }
  },
  {
    path: '/company/qrcode',
    component: () => import('../views/company/CompanyQrCodeView.vue'),
    meta: { authRole: 'company', title: '溯源码管理' }
  },
  {
    path: '/company/alert',
    component: () => import('../views/company/CompanyAlertView.vue'),
    meta: { authRole: 'company', title: '预警中心' }
  },
  {
    path: '/company/stats',
    component: () => import('../views/company/CompanyStatsView.vue'),
    meta: { authRole: 'company', title: '数据统计分析' }
  }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 攸县香干区块链溯源系统`;
  } else {
    document.title = '攸县香干区块链溯源系统';
  }
  
  // 权限验证
  const role = to.meta?.authRole;
  if (!role) {
    next();
    return;
  }
  const token = getToken(role);
  if (!token) {
    next(role === 'admin' ? '/admin/login' : '/company/login');
    return;
  }
  next();
});

export default router;

