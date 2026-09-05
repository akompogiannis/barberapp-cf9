import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    // Port 5173 is the origin already whitelisted in the backend's
    // `allowed.origins`, so CORS works with no extra configuration.
    port: 5173,
  },
});
