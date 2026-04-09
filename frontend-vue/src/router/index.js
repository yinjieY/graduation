import { createRouter, createWebHashHistory } from 'vue-router';
import AdminDashboardView from '../views/AdminView.vue';
import CompanyDashboardView from '../views/CompanyView.vue';
import AdminLoginView from '../views/AdminLoginView.vue';
import AdminRegisterView from '../views/AdminRegisterView.vue';
import CompanyLoginView from '../views/CompanyLoginView.vue';
import CompanyRegisterView from '../views/CompanyRegisterView.vue';
import ScanView from '../views/ScanView.vue';
import { getToken } from '../api/session';

const routes = [
  { path: '/', redirect: '/scan' },
  { path: '/scan', component: ScanView }
  ,{ path: '/admin/login', component: AdminLoginView }
  ,{ path: '/admin/register', component: AdminRegisterView }
  ,{ path: '/admin/dashboard', component: AdminDashboardView, meta: { authRole: 'admin' } }
  ,{ path: '/company/login', component: CompanyLoginView }
  ,{ path: '/company/register', component: CompanyRegisterView }
  ,{ path: '/company/dashboard', component: CompanyDashboardView, meta: { authRole: 'company' } }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

router.beforeEach((to, from, next) => {
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

