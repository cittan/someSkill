import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 开发期代理到后端，规避跨域；生产由 Nginx 承担同样的转发
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
