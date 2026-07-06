import { defineConfig, loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backend = env.VITE_BACKEND_URL || "http://127.0.0.1:8080";

  const stripBrowserOrigin = (proxy) => {
    proxy.on("proxyReq", (proxyReq) => {
      proxyReq.removeHeader("origin");
      proxyReq.removeHeader("referer");
    });
  };

  return {
    server: {
      host: "0.0.0.0",
      port: 4174,
      proxy: {
        "/api": {
          target: backend,
          changeOrigin: true,
          configure: stripBrowserOrigin,
        },
        "/v1": {
          target: backend,
          changeOrigin: true,
          configure: stripBrowserOrigin,
        },
      },
    },
    preview: {
      host: "0.0.0.0",
      port: 4174,
      proxy: {
        "/api": {
          target: backend,
          changeOrigin: true,
          configure: stripBrowserOrigin,
        },
        "/v1": {
          target: backend,
          changeOrigin: true,
          configure: stripBrowserOrigin,
        },
      },
    },
  };
});
