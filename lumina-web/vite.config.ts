import { defineConfig, loadEnv, type ProxyOptions } from 'vite';
import react from '@vitejs/plugin-react';

// 开发时把后端 API / Relay / Actuator 统统代理到本地 Spring Boot，
// 避免浏览器跨域。生产环境前端由后端同源托管，不会走代理。
// 可通过 VITE_BACKEND_URL 覆盖后端地址，例如 VITE_BACKEND_URL=http://127.0.0.1:9090。
//
// 注意：浏览器到 vite 是同源，但 vite 默认会把 Origin / Referer 原样转给
// 上游。Spring 的 CorsWebFilter 看到一个不在白名单里的 Origin 会直接 403，
// 所以这里主动把这两个头剥掉，让后端把请求当成"同源"来处理。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_URL || 'http://127.0.0.1:8080';

  const stripBrowserOrigin: ProxyOptions['configure'] = (proxy) => {
    proxy.on('proxyReq', (proxyReq) => {
      proxyReq.removeHeader('origin');
      proxyReq.removeHeader('referer');
    });
  };

  const apiLike: ProxyOptions = {
    target: backend,
    changeOrigin: true,
    configure: stripBrowserOrigin,
  };

  // relay SSE 长连接，额外禁用超时。
  const relayLike: ProxyOptions = {
    ...apiLike,
    timeout: 0,
    proxyTimeout: 0,
  };

  const proxy: Record<string, ProxyOptions> = {
    '/api': apiLike,
    '/v1': relayLike,
    '/v1beta': relayLike,
    '/actuator': apiLike,
  };

  return {
    plugins: [react()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy,
    },
    preview: {
      host: '0.0.0.0',
      port: 5173,
      proxy,
    },
  };
});
