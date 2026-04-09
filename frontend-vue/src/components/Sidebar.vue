<template>
  <div class="sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-header">
      <div class="logo" :class="{ collapsed: isCollapsed }">
        <h3 v-if="!isCollapsed">{{ role === 'admin' ? '管理员后台' : '商家工作台' }}</h3>
        <span v-else>{{ role === 'admin' ? '管' : '商' }}</span>
      </div>
      <button class="collapse-btn" @click="toggleCollapse">
        <span class="icon">{{ isCollapsed ? '>' : '<' }}</span>
      </button>
    </div>

    <nav class="sidebar-nav">
      <ul>
        <li v-for="item in navItems" :key="item.path" :class="{ active: currentPath === item.path }">
          <router-link :to="item.path">
            <span class="nav-icon">{{ item.icon }}</span>
            <span v-if="!isCollapsed" class="nav-text">{{ item.label }}</span>
          </router-link>
        </li>
      </ul>
    </nav>

    <div class="sidebar-footer">
      <button class="logout-btn" @click="logout">
        <span class="nav-icon">↪</span>
        <span v-if="!isCollapsed" class="nav-text">退出登录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { clearToken } from '../api/session';

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: (value) => ['admin', 'company'].includes(value)
  }
});

const router = useRouter();
const route = useRoute();
const isCollapsed = ref(false);

const navItems = computed(() => {
  if (props.role === 'admin') {
    return [
      { path: '/admin/dashboard', label: '工作台', icon: '📊' },
      { path: '/admin/enterprise', label: '企业管理', icon: '🏢' },
      { path: '/admin/feedback', label: '反馈处理', icon: '💬' },
      { path: '/admin/message', label: '系统消息', icon: '📋' }
    ];
  } else {
    return [
      { path: '/company/dashboard', label: '工作台', icon: '📊' },
      { path: '/company/auth', label: '资质认证', icon: '📄' },
      { path: '/company/feedback', label: '反馈管理', icon: '💬' },
      { path: '/company/message', label: '消息中心', icon: '📋' }
    ];
  }
});

const currentPath = computed(() => route.path);

function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value;
  localStorage.setItem(`sidebar_collapsed_${props.role}`, isCollapsed.value.toString());
}

function logout() {
  clearToken(props.role);
  router.push(props.role === 'admin' ? '/admin/login' : '/company/login');
}

onMounted(() => {
  const savedState = localStorage.getItem(`sidebar_collapsed_${props.role}`);
  if (savedState !== null) {
    isCollapsed.value = savedState === 'true';
  }
});
</script>

<style scoped>
.sidebar {
  width: 240px;
  height: 100vh;
  background: #1e293b;
  color: white;
  transition: all 0.3s ease;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #334155;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  font-weight: 700;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.logo h3 {
  margin: 0;
  font-size: 18px;
  color: #f8fafc;
}

.logo.collapsed {
  font-size: 20px;
}

.collapse-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background: #334155;
  color: #f8fafc;
}

.sidebar-nav {
  flex: 1;
  padding: 20px 0;
}

.sidebar-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar-nav li {
  margin: 4px 0;
}

.sidebar-nav a {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s ease;
  border-radius: 0 100px 100px 0;
  margin: 0 12px;
}

.sidebar-nav a:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #f8fafc;
}

.sidebar-nav li.active a {
  background: rgba(59, 130, 246, 0.2);
  color: #f8fafc;
  border-left: 3px solid #3b82f6;
}

.nav-icon {
  font-size: 18px;
  margin-right: 12px;
  min-width: 20px;
  text-align: center;
}

.nav-text {
  transition: all 0.3s ease;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #334155;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #f8fafc;
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }
  
  .sidebar.open {
    transform: translateX(0);
  }
}
</style>