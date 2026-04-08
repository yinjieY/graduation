<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">QSGuard 香干溯源平台</div>
      <nav class="tabs">
        <RouterLink to="/admin">管理员</RouterLink>
        <RouterLink to="/company">商家</RouterLink>
        <RouterLink to="/scan">扫码页</RouterLink>
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

