import { createRouter, createWebHashHistory } from 'vue-router';
import AdminView from '../views/AdminView.vue';
import CompanyView from '../views/CompanyView.vue';
import ScanView from '../views/ScanView.vue';

const routes = [
  { path: '/', redirect: '/admin' },
  { path: '/admin', component: AdminView },
  { path: '/company', component: CompanyView },
  { path: '/scan', component: ScanView }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

export default router;

