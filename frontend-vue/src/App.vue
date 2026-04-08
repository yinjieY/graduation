<template>
  <div>
    <header class="topbar">
      <strong>香干溯源平台</strong>
      <nav>
        <RouterLink to="/admin">管理员</RouterLink>
        <RouterLink to="/company">商家</RouterLink>
        <RouterLink to="/scan">扫码页</RouterLink>
      </nav>
    </header>
    <main class="page">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router';
</script>

