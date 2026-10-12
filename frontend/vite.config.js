import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Requests to /api are forwarded to the Spring Boot backend, so no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': process.env.API_URL || 'http://localhost:8080',
    },
  },
});
