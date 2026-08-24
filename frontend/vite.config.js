import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // Autorise tous les hôtes externes (Ngrok, etc.)
    allowedHosts: true,

    proxy: {
      // Proxy pour GLPI
      '/api-glpi': {
        target: 'http://localhost/glpi',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api-glpi/, '')
      },
      
      // Proxy pour Spring Boot (SQLite)
      '/api-sqlite': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // Si vos routes dans Spring Boot commencent déjà par /api (ex: /api/users),
        // on garde le préfixe en réécrivant proprement.
        rewrite: (path) => path.replace(/^\/api-sqlite/, '')
      }
    }
  }
})