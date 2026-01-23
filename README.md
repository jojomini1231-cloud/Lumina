# Lumina

Lumina 是一个高性能、轻量级的 LLM API 网关服务，旨在为多个 AI 模型提供商提供统一、安全且具备故障转移能力的接口。

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen.svg)

## 🚀 功能特性

- **多提供商支持**：原生支持 Anthropic (Claude)、OpenAI (GPT)、Google (Gemini) 等主流提供商。
- **统一 API 接口**：提供与 OpenAI/Anthropic/Gemini 兼容的标准端点，无缝替换原有接口。
- **流式响应 (SSE)**：全异步架构支持流式输出，确保极致的响应速度。
- **智能负载均衡**：基于提供商状态和评分的智能路由。
- **高可用机制**：内置断路器 (Circuit Breaker) 和自动故障转移，检测到提供商异常时自动切换。
- **灵活的权限管理**：
  - 基于 JWT 的管理员后台认证。
  - 支持多 key 管理。
- **可视化管理界面**：基于 React + Vite 的现代化控制台，直观管理模型、分组和日志。
- **高性能架构**：基于 Spring WebFlux 响应式编程模型，单实例即可处理高并发请求。
- **零依赖部署**：支持 SQLite 且 Docker 镜像内置 Redis，实现“开箱即用”。

## 🛠️ 技术栈

- **后端**：Spring Boot 3.5.9, Spring WebFlux, Spring Security
- **数据访问**：MyBatis Plus, HikariCP
- **数据库**：MySQL 8.0 / SQLite (自动适配)
- **缓存**：Redis (用于状态记录和频率限制)
- **前端**：React 18, Vite, TypeScript, Pnpm
- **工具**：OkHttp 4.12, JJWT, Lombok

## 📦 快速开始

### 方式一：Docker 部署（推荐）

Lumina 的 Docker 镜像已内置 Redis，您只需关心数据库配置。

#### 1. 使用 SQLite (零配置启动)
```bash
docker compose up -d
```
*数据将保存在容器映射的 `./data` 目录中。*

#### 2. 使用 MySQL (生产推荐)
```bash
docker compose -f docker-compose-mysql.yml up -d
```

**默认凭据**：
- **管理后台**：`http://localhost:8080`
- **用户名**：`admin`
- **密码**：`admin123`

---

### 方式二：本地开发部署

#### 环境要求
- JDK 17+
- Maven 3.6+
- Redis 6.0+ (本地需运行 Redis 服务)
- MySQL 8.0+ 或 SQLite

#### 1. 后端启动
```bash
# 克隆项目
git clone <repository-url>
cd lumina

# 编译并运行 (默认使用 SQLite)
mvn clean package -DskipTests
java -jar target/lumina-0.0.1-SNAPSHOT.jar
```

#### 2. 前端启动
```bash
cd lumina-web
pnpm install
pnpm dev
```

## 🔌 API 使用指南

### 兼容性端点

| 原始 API | Lumina 端点 | 说明 |
|----------|------------|------|
| Anthropic | `POST /v1/messages` | 支持流式与非流式 |
| OpenAI | `POST /v1/chat/completions` | 支持流式与非流式 |
| OpenAI | `POST /v1/responses` | 支持新版 Realtime/Response API 格式 |
| Gemini | `POST /v1beta/models/*` | 完美匹配 Google API 路径 |
| 通用 | `GET /v1/models` | 列出所有可用的模型分组 |

### 调用示例
使用 Lumina 生成的 API Key 调用：
```bash
curl http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer LMN_YOUR_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4",
    "messages": [{"role": "user", "content": "Hello!"}],
    "stream": true
  }'
```

## 📂 项目结构

```text
lumina/
├── src/main/java/com/lumina/
│   ├── config/          # 系统配置（安全、数据源、初始化）
│   ├── controller/      # API 控制器（包括 Relay 转发核心）
│   ├── service/impl/    # 核心逻辑（各提供商的 Executor 实现）
│   ├── state/           # 评分系统与断路器逻辑
│   └── filter/          # 认证过滤器
├── lumina-web/          # 前端项目
│   ├── src/components/  # 业务组件与页面视图
│   ├── src/services/    # API 请求封装
│   └── src/utils/       # 通用工具
└── target/              # 编译输出
```

## 📅 路线图

- [x] 支持流式响应代理
- [ ] 支持多负载均衡模式
- [ ] 支持供应商多KEY管理
- [ ] 完善请求缓存机制
- [ ] 细粒度的速率限制 (Rate Limiting)
- [ ] 支持更多提供商 (Cohere, DeepSeek, Llama.cpp)

## 📄 许可证
本项目采用 [AGPL-3.0 license](LICENSE) 开源。