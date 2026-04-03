<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# Lumina Web

Lumina Web 是 Lumina Gateway 的前端管理控制台，用于管理 LLM 上游供应商、模型分组、价格数据、请求日志以及熔断与运行态观测信息。

它面向的不是普通内容展示场景，而是 AI 网关的运维与配置后台。

## 项目定位

该项目为 Lumina Gateway 提供可视化管理界面，主要解决以下问题：

- 统一管理多个 LLM Provider
- 按模型与分组进行路由配置
- 观察请求流量、Token 消耗、成本与成功率
- 查看请求日志与请求详情
- 管理 API Key、用户资料、主题与语言
- 查看并手动干预熔断器状态

## 核心功能

### 1. 登录与认证

- 用户名密码登录
- 基于本地 Token 的登录态恢复
- 未授权时自动清理会话并返回登录页

### 2. 仪表盘

- 总请求数、总 Token、总费用、平均延迟、成功率概览
- 24 小时请求流量趋势图
- 模型 Token 使用分布图
- Provider 排名统计
- 运行态观测面板
  - 缓存命中率
  - 熔断状态
  - Failover 指标
  - Bulkhead 拒绝数
  - 日志队列状态

### 3. 供应商管理

- 分页查看 Provider 列表
- 新增、编辑、删除 Provider
- 启用 / 停用 Provider
- 配置 Provider 类型、Base URL、API Key、模型列表
- 同步上游模型列表

当前支持的 Provider 类型：

- OpenAI Chat
- OpenAI Response
- Anthropic
- Gemini
- New API

### 4. 分组管理

- 创建和维护模型分组
- 为每个分组选择多个 Provider + Model 目标
- 配置首 Token 超时时间
- 支持 SAPR、Round Robin 等路由模式的数据结构
- 检测无效目标配置

### 5. 价格管理

- 查看模型价格分页列表
- 按模型名称搜索
- 同步模型价格数据
- 展示输入/输出单价、上下文长度、最大输出长度、能力标签

### 6. 日志系统

- 分页查看请求日志
- 自动刷新日志
- 查看请求详情
- 按需加载请求体与响应体内容
- 查看错误信息、Token、耗时与成本

### 7. 设置中心

- 中英文切换
- 深色 / 浅色主题切换
- 用户资料更新
- API Token 管理
- 熔断器管理与手动控制

## 技术栈

### 前端框架

- React 19
- TypeScript 5
- Vite 5

### UI 与可视化

- Tailwind CSS 风格原子类
- lucide-react
- recharts

### 应用结构

- React Context 管理全局认证、语言、主题状态
- 基于 `fetch` 的统一请求封装
- `services` 目录按业务领域拆分 API 调用
- 无前端路由，采用单页应用内部视图切换

## 目录结构

```text
lumina-web/
├── components/         # 页面与通用组件
├── services/           # 业务接口封装
├── utils/              # 通用工具
├── App.tsx             # 应用入口与视图切换
├── constants.ts        # 项目元信息
├── types.ts            # 全局类型定义
├── index.tsx           # React 挂载入口
├── index.html          # HTML 模板与样式注入
├── vite.config.ts      # Vite 配置
└── package.json        # 依赖与脚本
```

## 本地运行

### 前置条件

- Node.js 18+
- 已启动的 Lumina Gateway 后端服务

### 安装依赖

```bash
npm install
```

如果你使用 pnpm，也可以执行：

```bash
pnpm install
```

### 启动开发环境

```bash
npm run dev
```

默认情况下，前端开发服务运行在：

- `http://localhost:5173`

## 后端接口说明

当前前端默认请求以下后端地址：

```text
http://127.0.0.1:8080/api/v1
```

该地址定义在：

- `utils/request.ts`

如果你的 Lumina Gateway 后端地址不同，需要同步修改对应的 `baseURL` 配置。

## 可用脚本

```bash
npm run dev
npm run build
npm run preview
```

说明：

- `dev`：启动本地开发服务器
- `build`：执行 TypeScript 编译并构建产物
- `preview`：本地预览构建结果

## 主要页面

| 页面 | 说明 |
| --- | --- |
| Dashboard | 展示网关整体运行情况与运行态观测 |
| Providers | 管理上游供应商与模型 |
| Groups | 配置分组和路由目标 |
| Pricing | 查看模型价格与能力信息 |
| Logs | 查询请求日志与详细内容 |
| Settings | 管理账号、Token、主题、语言与熔断器 |

## 接口模块

`services/` 目录按领域拆分，便于维护：

- `dashboardService.ts`
- `providerService.ts`
- `groupService.ts`
- `modelService.ts`
- `logService.ts`
- `tokenService.ts`
- `userService.ts`
- `circuitBreakerService.ts`

## 当前适用场景

该项目适合以下场景：

- 自建 LLM 网关后台管理
- 多模型、多供应商统一接入
- AI 请求可观测性与成本分析
- Provider 故障切换与熔断运维

## 版本信息

- Project: Lumina
- Version: 0.4.0

## 说明

当前仓库中的 README 已基于现有代码结构和功能进行了重写，内容与项目实际实现保持一致，不再依赖 AI Studio 或 `GEMINI_API_KEY` 配置。
