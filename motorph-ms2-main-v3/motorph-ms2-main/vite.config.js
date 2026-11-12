import { defineConfig } from 'vite';
import tailwindcss from '@tailwindcss/vite';
import { resolve } from 'node:path';

const apiTarget = process.env.API_PROXY_TARGET || 'http://127.0.0.1:5420';

export default defineConfig({
  root: 'web',
  plugins: [tailwindcss()],
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
    emptyOutDir: true,
    rollupOptions: {
      input: Object.fromEntries(['Landing', 'Index', 'Help', 'Privacy', 'NotFound']
        .map(name => [name, resolve(process.cwd(), `web/b_basic/${name}.html`)]))
    }
  }
});
