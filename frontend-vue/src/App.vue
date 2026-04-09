<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <div class="brand-icon">🏮</div>
        <div class="brand-text">攸县香干溯源平台</div>
      </div>
      <nav class="tabs">
        <RouterLink to="/scan" class="tab-link">
          <span class="tab-icon">📱</span>
          <span class="tab-text">扫码页面</span>
        </RouterLink>
        <RouterLink to="/admin/login" class="tab-link">
          <span class="tab-icon">🛡️</span>
          <span class="tab-text">管理员入口</span>
        </RouterLink>
        <RouterLink to="/company/login" class="tab-link">
          <span class="tab-icon">🏭</span>
          <span class="tab-text">商家入口</span>
        </RouterLink>
      </nav>
    </header>
    <main class="page container">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
}

.topbar {
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 0 24px 0 120px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
  transition: padding-left 0.3s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #0e6b52;
  font-family: 'Noto Sans SC', sans-serif;
  animation: slideInLeft 0.6s ease-out;
}

.brand-icon {
  font-size: 24px;
  animation: spin 3s linear infinite;
}

.brand-text {
  background: linear-gradient(135deg, #0e6b52 0%, #1a8f6e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 800;
  letter-spacing: 1px;
}

.tabs {
  display: flex;
  gap: 8px;
  animation: slideInRight 0.6s ease-out;
}

.tab-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  position: relative;
}

.tab-link:hover {
  background: #f8fafc;
  color: #334155;
  border-color: #e2e8f0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.tab-link.router-link-active {
  background: #0e6b52;
  color: white;
  border-color: #0e6b52;
  box-shadow: 0 4px 12px rgba(14, 107, 82, 0.2);
}

.tab-icon {
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.page {
  padding: 24px;
  min-height: calc(100vh - 64px);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .topbar {
    padding: 0 16px;
  }
  
  .brand {
    font-size: 16px;
  }
  
  .brand-icon {
    font-size: 20px;
  }
  
  .tab-link {
    padding: 6px 12px;
    font-size: 12px;
  }
  
  .tab-text {
    display: none;
  }
  
  .page {
    padding: 16px;
  }
}
</style>

