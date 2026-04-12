<template>
  <div class="sidebar" :class="{ collapsed: isCollapsed }">
    <div class="sidebar-header">
      <div class="logo" :class="{ collapsed: isCollapsed }">
        <h3 v-if="!isCollapsed">{{ role === 'admin' ? '管理员后台' : '商家工作台' }}</h3>
        <span v-else class="logo-icon-collapsed">{{ role === 'admin' ? '🛡️' : '🏭' }}</span>
      </div>
      <button class="collapse-btn" @click="toggleCollapse" :aria-label="isCollapsed ? '展开侧边栏' : '折叠侧边栏'">
        <span class="icon" :class="{ rotated: isCollapsed }">{{ isCollapsed ? '>' : '<' }}</span>
      </button>
    </div>

    <nav class="sidebar-nav">
      <ul>
        <li v-for="item in navItems" :key="item.path" :class="{ active: currentPath === item.path }">
          <router-link :to="item.path" :title="isCollapsed ? item.label : ''">
            <span class="nav-icon">{{ item.icon }}</span>
            <span v-if="!isCollapsed" class="nav-text">{{ item.label }}</span>
          </router-link>
        </li>
      </ul>
    </nav>

    <div class="sidebar-footer">
      <button class="logout-btn" @click="logout" :title="isCollapsed ? '退出登录' : ''" aria-label="退出登录">
        <span class="nav-icon">↪</span>
        <span v-if="!isCollapsed" class="nav-text">退出登录</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { clearToken } from '../api/session';
import { useSidebarState } from '../composables/useSidebar';

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: (value) => ['admin', 'company'].includes(value)
  }
});

const router = useRouter();
const route = useRoute();
const { isCollapsed, toggleCollapse } = useSidebarState();

const navItems = computed(() => {
  if (props.role === 'admin') {
    return [
      { path: '/admin/dashboard', label: '工作台', icon: '📊' },
      { path: '/admin/enterprise', label: '企业管理', icon: '🏢' },
      { path: '/admin/batch-review', label: '批次审核', icon: '📦' },
      { path: '/admin/qrcode', label: '二维码管理', icon: '📱' },
      { path: '/admin/feedback', label: '反馈处理', icon: '💬' },
      { path: '/admin/message', label: '系统消息', icon: '📋' }
    ];
  } else {
    return [
      { path: '/company/dashboard', label: '工作台', icon: '📊' },
      { path: '/company/info', label: '企业信息', icon: '🏭' },
      { path: '/company/batch', label: '生产批次', icon: '📦' },
      { path: '/company/qrcode', label: '溯源码管理', icon: '📱' },
      { path: '/company/alert', label: '预警中心', icon: '⚠' },
      { path: '/company/stats', label: '数据统计', icon: '📈' },
      { path: '/company/auth', label: '资质认证', icon: '📄' },
      { path: '/company/feedback', label: '反馈管理', icon: '💬' },
      { path: '/company/message', label: '消息中心', icon: '📋' }
    ];
  }
});

const currentPath = computed(() => route.path);

function logout() {
  clearToken(props.role);
  router.push(props.role === 'admin' ? '/admin/login' : '/company/login');
}
</script>

<style scoped>
/* 侧边栏容器 */
.sidebar {
  width: 240px;
  height: calc(100vh - 64px);
  background: #1e293b;
  color: white;
  transition: all 0.3s ease;
  position: fixed;
  left: 0;
  top: 64px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 折叠状态 */
.sidebar.collapsed {
  width: 64px;
}

/* 侧边栏头部 */
.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #334155;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  box-sizing: border-box;
}

/* Logo 样式 */
.logo {
  font-weight: 700;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  flex: 1;
  white-space: nowrap; /* 防止折叠时文字换行 */
}

.logo h3 {
  margin: 0;
  font-size: 16px;
  color: #f8fafc;
  font-family: 'Noto Sans SC', sans-serif;
  transition: all 0.3s ease;
  opacity: 1;
  transform: translateX(0);
}

.sidebar.collapsed .logo h3 {
  opacity: 0;
  transform: translateX(-10px);
}

.logo.collapsed {
  justify-content: center;
}

.logo-icon-collapsed {
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  opacity: 0;
  transform: scale(0.8);
}

.sidebar.collapsed .logo-icon-collapsed {
  opacity: 1;
  transform: scale(1);
}

/* 折叠按钮 */
.collapse-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-left: 8px;
  outline: none;
}

.collapse-btn:hover {
  background: #334155;
  color: #f8fafc;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transform: scale(1.05);
}

.collapse-btn:active {
  transform: scale(0.95);
}

.collapse-btn .icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  font-weight: bold;
  transition: transform 0.3s ease;
}

.collapse-btn .icon.rotated {
  transform: rotate(180deg);
}

/* 导航区域 */
.sidebar-nav {
  flex: 1;
  padding: 16px 0;
  overflow-y: auto;
}

.sidebar-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar-nav li {
  margin: 2px 0;
}

/* 导航链接 */
.sidebar-nav a {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.2s ease;
  border-radius: 8px;
  margin: 0 12px;
  position: relative;
  white-space: nowrap; /* 防止文字换行 */
  overflow: hidden;
}

.sidebar-nav a:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #f8fafc;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
  transform: translateX(4px);
}

.sidebar-nav li.active a {
  background: rgba(59, 130, 246, 0.2);
  color: #f8fafc;
  border-left: 3px solid #3b82f6;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

/* 导航图标 */
.nav-icon {
  font-size: 16px;
  margin-right: 12px;
  min-width: 20px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

/* 导航文本 */
.nav-text {
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  opacity: 1;
  transform: translateX(0);
}

.sidebar.collapsed .nav-text {
  opacity: 0;
  transform: translateX(-10px);
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #334155;
}

/* 退出按钮 */
.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 16px;
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  margin: 0 12px;
  white-space: nowrap;
  outline: none;
  overflow: hidden;
}

.logout-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #f8fafc;
  box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2);
  transform: translateX(4px);
}

.logout-btn:active {
  transform: translateX(2px);
}

/* ---------------- 折叠状态下的优化调整 ---------------- */
.sidebar.collapsed .sidebar-nav a {
  justify-content: center;
  padding: 12px;
  margin: 0 8px; /* 调整间隙以防止超出64px宽度 */
  transform: none;
}

.sidebar.collapsed .sidebar-nav a:hover {
  transform: none;
  background: rgba(59, 130, 246, 0.15);
}

.sidebar.collapsed .logout-btn {
  justify-content: center;
  padding: 12px;
  margin: 0 8px; /* 调整间隙以防止超出64px宽度 */
  transform: none;
}

.sidebar.collapsed .logout-btn:hover {
  transform: none;
  background: rgba(239, 68, 68, 0.15);
}

.sidebar.collapsed .nav-icon {
  margin-right: 0;
  font-size: 18px;
}

/* 滚动条样式 */
.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: #1e293b;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: #334155;
  border-radius: 2px;
  transition: background 0.2s ease;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: #475569;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
  }
}

/* 键盘焦点样式 */
.collapse-btn:focus,
.logout-btn:focus,
.sidebar-nav a:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 2px;
}

/* 动画效果增强 */
.sidebar-nav li {
  transition: all 0.3s ease;
}

.sidebar.collapsed .sidebar-nav li {
  transform: scale(0.95);
}

.sidebar.collapsed .sidebar-nav li:hover {
  transform: scale(1);
}
</style>