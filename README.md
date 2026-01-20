# Lumina

Lumina 是一个高性能的 LLM API 网关服务，提供统一的接口来管理和转发多个 AI 模型提供商的请求。

## 功能特性

- **多提供商支持**：支持 Anthropic、OpenAI、Gemini 等主流 AI 模型提供商
- **统一 API 接口**：提供标准化的 API 端点，简化多模型集成
- **智能负载均衡**：基于提供商状态和评分的智能路由
- **断路器机制**：自动检测和隔离故障提供商，提高系统可用性
- **用户认证**：基于 JWT 的安全认证机制
- **API Key 管理**：灵活的 API Key 创建和管理
- **请求日志**：完整的请求追踪和日志记录
- **分组管理**：支持模型分组和批量管理
- **实时监控**：提供仪表板和统计数据
- **响应式架构**：基于 WebFlux 的高性能异步处理

## 技术栈

- **框架**：Spring Boot 3.5.9
- **响应式编程**：Spring WebFlux
- **数据库**：MySQL 8.0 + MyBatis Plus
- **缓存**：Redis
- **安全**：Spring Security + JWT
- **连接池**：HikariCP
- **HTTP 客户端**：OkHttp
- **Java 版本**：17

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 安装步骤

1. 克隆项目
```bash
git clone <repository-url>
cd lumina
```

2. 配置数据库
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE lumina CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. 修改配置文件
编辑 `src/main/resources/application.yaml`，配置数据库和 Redis 连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lumina
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

4. 构建项目
```bash
mvn clean package
```

5. 运行应用
```bash
java -jar target/lumina-0.0.1-SNAPSHOT.jar
```

应用将在 `http://localhost:8080` 启动。

## API 端点

### Anthropic Messages API
```
POST /v1/messages
```

### OpenAI Chat Completions API
```
POST /v1/chat/completions
```

### OpenAI Responses API
```
POST /v1/responses
```

### Gemini Models API
```
POST /v1beta/models/{modelAction}
```

### 管理接口
- 用户管理：`/api/v1/user/*`
- API Key 管理：`/api/v1/apikey/*`
- 提供商管理：`/api/v1/provider/*`
- 模型管理：`/api/v1/model/*`
- 分组管理：`/api/v1/group/*`
- 仪表板：`/api/v1/dashboard/*`

## 配置说明

### JWT 配置
```yaml
lumina:
  auth:
    jwt:
      secret: your-secret-key
      expiration: 86400000  # 24小时
      refresh-expiration: 604800000  # 7天
```

### 监控端点
应用集成了 Spring Boot Actuator，可通过以下端点监控应用状态：
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## 开发

### 项目结构
```
src/main/java/com/lumina/
├── config/          # 配置类
├── controller/      # 控制器
├── dto/            # 数据传输对象
├── entity/         # 实体类
├── exception/      # 异常处理
├── filter/         # 过滤器
├── logging/        # 日志处理
├── mapper/         # MyBatis Mapper
├── service/        # 业务逻辑
├── state/          # 状态管理（断路器、提供商状态）
└── util/           # 工具类
```

### 构建命令
```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包
mvn package

# 跳过测试打包
mvn package -DskipTests
```

## 许可证

[待添加]

## 贡献

欢迎提交 Issue 和 Pull Request。
