import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// `npm run build` writes exactly three files into Spring Boot's static folder:
// index.html, app.js and app.css. Those are the only page files rule 2 of SecurityConfig opens,
// so the build must never produce hashed chunks or extra assets.
// `npm run dev` serves the page on :5173 and forwards API calls to Spring Boot on :8080,
// so the browser still sees one origin and no CORS bean is needed.
const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    cssCodeSplit: false,
    modulePreload: false,
    assetsInlineLimit: 100_000_000,
    rollupOptions: {
      output: {
        entryFileNames: 'app.js',
        assetFileNames: 'app.[ext]',
        inlineDynamicImports: true,
      },
    },
  },
  server: {
    proxy: {
      '/api': backend,
      '/actuator': backend,
      '/v3': backend,
      '/swagger-ui': backend,
    },
  },
});
