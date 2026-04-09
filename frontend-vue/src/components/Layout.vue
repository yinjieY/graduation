<template>
  <div class="layout" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <Sidebar :role="role" />
    <div class="main-content" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <div class="content-wrapper">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import Sidebar from './Sidebar.vue';

const props = defineProps({
  role: {
    type: String,
    required: true,
    validator: (value) => ['admin', 'company'].includes(value)
  }
});

const route = useRoute();
const isSidebarCollapsed = ref(false);

function checkSidebarState() {
  const savedState = localStorage.getItem(`sidebar_collapsed_${props.role}`);
  isSidebarCollapsed.value = savedState === 'true';
}

// 监听本地存储变化，同步侧边栏状态
window.addEventListener('storage', (e) => {
  if (e.key === `sidebar_collapsed_${props.role}`) {
    isSidebarCollapsed.value = e.newValue === 'true';
  }
});

onMounted(() => {
  checkSidebarState();
});
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
}

.main-content.sidebar-collapsed {
  margin-left: 90px;
}

.content-wrapper {
  padding: 32px;
  min-height: 100vh;
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