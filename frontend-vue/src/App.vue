<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">攸县香干溯源平台</div>
      <nav class="tabs">
        <RouterLink to="/scan">扫码页面</RouterLink>
        <RouterLink to="/admin/login">管理员入口</RouterLink>
        <RouterLink to="/company/login">商家入口</RouterLink>
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

