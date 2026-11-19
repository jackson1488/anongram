import { defineConfig } from 'vite';

export default defineConfig({
  base: '/anongram/',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    target: 'es2022',
  },
});
