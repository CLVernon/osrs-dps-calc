import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Deployed as a GitHub Pages project site
export default defineConfig({
  base: '/osrs-dps-calc/',
  plugins: [react()],
});
