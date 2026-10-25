import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  base: "/app/",
  plugins: [react()],
  server: {
    proxy: {
      "/maps/amap": { target: "http://127.0.0.1:8080", changeOrigin: false },
      "/api": { target: "http://127.0.0.1:8080", changeOrigin: false },
      "/share": { target: "http://127.0.0.1:8080", changeOrigin: false },
    },
  },
  build: { outDir: "dist", emptyOutDir: true },
});
