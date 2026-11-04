import { defineConfig } from 'vite';

const apiTarget = process.env.API_PROXY_TARGET || 'http://127.0.0.1:8080';

export default defineConfig({
  root: 'web',
  envDir: '..',
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: ['.e2b.app'],
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../dist',
    emptyOutDir: true
  }
});
