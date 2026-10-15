import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// /api calls are proxied to the Spring Boot backend, so no CORS setup is needed in dev
export default defineConfig(({ mode }) => {
  const { HELPDESK_API_TARGET } = loadEnv(mode, '.', 'HELPDESK_API_TARGET');
  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: { '/api': HELPDESK_API_TARGET || 'http://localhost:8080' },
    },
  };
});
