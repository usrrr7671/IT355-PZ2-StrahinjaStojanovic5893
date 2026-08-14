import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Zahtevi ka /api se u razvoju preusmeravaju na Spring Boot na portu 8080.
// Time frontend i backend za pregledac deluju kao isto poreklo, pa u razvoju
// nema CORS-a niti potrebe da se osnovna adresa API-ja upisuje u kod.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
