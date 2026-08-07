import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 백엔드 SecurityConfig의 CORS 허용 origin(http://localhost:3000)과 맞춤
    port: 3000,
  },
})
