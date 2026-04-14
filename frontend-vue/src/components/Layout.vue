<template>
  <div class="layout">
    <Sidebar :role="role" :unreadCount="unreadCount" />
    <div class="main-content" :class="{ 'sidebar-collapsed': isCollapsed }">
      <div class="content-wrapper">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { provideSidebarState } from '../composables/useSidebar';
import Sidebar from './Sidebar.vue';

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: (value) => ['admin', 'company'].includes(value)
  },
  unreadCount: {
    type: Number,
    default: 0
  }
});

// 提供侧边栏状态管理
const { isCollapsed } = provideSidebarState(props.role);
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: #f1f5f9;
}

.main-content {
  flex: 1;
  margin-left: 240px;
  transition: all 0.3s ease;
  min-height: 100vh;
  background: #ffffff;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.05);
}

/* 修正宽度：保持和侧边栏折叠后的 64px 宽度一致 */
.main-content.sidebar-collapsed {
  margin-left: 64px;
}

.content-wrapper {
  padding: 32px;
  min-height: 100vh;
  transition: all 0.3s ease;
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
  }

  .content-wrapper {
    padding: 20px;
  }
}
</style>