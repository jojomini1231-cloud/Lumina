# Lumina Open Web

开放用户端首页，用于通过密钥查询请求次数、已使用次数和可用模型。

## 开发启动

默认请求同源接口：

```text
/api/v1/public/key-usage
/v1/models
```

开发服务器会把 `/api` 和 `/v1` 代理到 `http://127.0.0.1:8080`。

```bash
pnpm install
pnpm run dev
```

打开：

```text
http://127.0.0.1:4174
```

如果确实需要跨域直连其他接口域名，可以在 `index.html` 引入 `app.js` 之前设置，但目标后端必须放行 CORS：

```html
<script>
  window.LUMINA_PUBLIC_API_ORIGIN = 'https://ai.luminex.chat';
</script>
```

## 接口

```text
POST https://ai.luminex.chat/api/v1/public/key-usage
GET https://ai.luminex.chat/v1/models
```

## 静态托管

如果不用 Vite，也可以直接静态托管 `dist` 目录。生产环境建议在 Nginx 把 `/api` 和 `/v1` 反代到后端，避免浏览器跨域。
