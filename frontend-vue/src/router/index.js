import { createRouter, createWebHashHistory } from 'vue-router';
import AdminView from '../views/AdminView.vue';
import CompanyView from '../views/CompanyView.vue';
import ScanView from '../views/ScanView.vue';
import { getToken } from '../api/session';

const routes = [
  { path: '/', redirect: '/admin' },
  { path: '/admin', component: AdminView, meta: { authRole: 'admin' } },
  { path: '/company', component: CompanyView, meta: { authRole: 'company' } },
  { path: '/scan', component: ScanView }
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
  // allow entering role page without token only when explicitly asking for login view state
  if (!token && to.query.auth !== 'login') {
    next({ path: to.path, query: { ...to.query, auth: 'login' } });
    return;
  }
  next();
});

export default router;

