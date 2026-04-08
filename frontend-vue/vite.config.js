import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  base: '/trace/app/',
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:9090',
      '/alert': 'http://localhost:9090',
      '/scan': 'http://localhost:9090',
      '/trace': 'http://localhost:9090'
    }
  }
});

