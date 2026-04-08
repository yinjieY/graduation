import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ command }) => ({
  plugins: [vue()],
  // Dev must use root base, otherwise /trace/app/* hits proxy '/trace' and returns 401 from backend.
  base: command === 'serve' ? '/' : '/trace/app/',
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:9090',
      '/alert': 'http://localhost:9090',
      '/scan': 'http://localhost:9090',
      '/trace': 'http://localhost:9090'
    }
  }
}));

