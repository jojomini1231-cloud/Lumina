# Lumina 熔断/容错机制优化方案

本文针对 Lumina 当前"按 Provider 维度"的轻量熔断实现，给出一套更完整、可配置、可观测、可扩展、并兼顾高性能的容错方案。目标是：在不显著增加请求路径开销的前提下，提高稳定性与恢复速度，并为后续多实例/分布式部署预留空间。

---

## 0. 现状分析（基于代码审查）

### 0.1 核心类与职责

| 类名 | 路径 | 职责 |
|------|------|------|
| `CircuitBreaker` | `state/CircuitBreaker.java` | 熔断状态判定与切换 |
| `CircuitState` | `state/CircuitState.java` | 状态枚举：`CLOSED/OPEN/HALF_OPEN` |
| `ProviderRuntimeState` | `state/ProviderRuntimeState.java` | 运行时状态数据模型 |
| `ProviderScoreCalculator` | `state/ProviderScoreCalculator.java` | 动态评分计算 |
| `ProviderStateRegistry` | `state/ProviderStateRegistry.java` | 状态管理容器（ConcurrentHashMap） |
| `FailoverService` | `service/FailoverService.java` | Provider 选择与故障转移 |
| `ProviderStatsPersistenceJob` | `state/ProviderStatsPersistenceJob.java` | 定期持久化（每10s） |

### 0.2 当前实现要点

**熔断器参数（硬编码）**：
```java
// CircuitBreaker.java
private static final int FAILURE_THRESHOLD = 5;       // 失败阈值
private static final long OPEN_DURATION_MS = 30_000;  // OPEN 冷却时间 30s
```

**评分计算参数**：
```java
// ProviderScoreCalculator.java
private static final double ALPHA = 0.4;                        // EMA 平滑系数
private static final double LATENCY_SAFE_THRESHOLD_MS = 5000.0; // 延迟安全阈值
private static final double LATENCY_MAX_THRESHOLD_MS = 30000.0; // 延迟最大阈值
private static final int WINDOW_SIZE = 20;                      // 滑动窗口大小
private static final int MIN_REQUESTS_FOR_SCORE = 15;           // 最少请求数
```

**评分公式**：
```java
score = successRateEma * 70 - latencyPenalty * 20 - recentFailureRate * 10
// 范围：1.0 ~ 100.0
```

**选择算法参数**：
```java
// FailoverService.java
private static final int TOP_K = 3;          // 取评分最高的 3 个
private static final double SOFTMAX_T = 10.0; // Softmax 温度参数
```

**状态转换逻辑**：
- `CLOSED → OPEN`：`failureRequests >= 5 && score < 40`
- `OPEN → HALF_OPEN`：超过 30s 后首次 `allowRequest()` 自动切换
- `HALF_OPEN → CLOSED`：`onSuccess()` 调用时立即切换并清零 `failureRequests`
- `HALF_OPEN → OPEN`：`onFailure()` + 满足触发条件

---

## 1. 问题清单

### 1.1 熔断触发条件不够健壮

**问题**：
- 以 `score < 40` 作为熔断条件的一部分，属于"间接指标"；score 的波动可能导致误开/误关
- `failureRequests` 在成功后被清零，语义是"阶段累计失败"，不是"连续失败"
- 缺少"最小请求数（minimum number of calls）"门槛，冷启动或低流量时容易误判

**代码定位**：
```java
// CircuitBreaker.java:31-32
if (stats.getFailureRequests().get() >= FAILURE_THRESHOLD
        && stats.getScore() < 40) {
```

### 1.2 HALF_OPEN 行为过于粗糙

**问题**：
- HALF_OPEN 等同于"完全放行"，可能在恢复期被瞬间打爆
- 缺少探测并发限制、探测次数、成功阈值/失败阈值等控制参数

**代码定位**：
```java
// CircuitBreaker.java:13-18 - 没有探测限制
if (stats.getCircuitState() == CircuitState.OPEN) {
    if (System.currentTimeMillis() - stats.getCircuitOpenedAt() > OPEN_DURATION_MS) {
        stats.setCircuitState(CircuitState.HALF_OPEN);
        return true;  // 直接放行，无限制
    }
}
```

### 1.3 OPEN 冷却时间固定

**问题**：
- 固定 30s 不适配不同类型故障（瞬时抖动 vs 长时间不可用）
- 缺少指数退避（Exponential Backoff）+ 抖动（Jitter），大规模失败时容易形成同步风暴

### 1.4 指标与分类不足

**问题**：
- "失败"未按类型分类：超时、连接失败、5xx、限流、4xx 等应采用不同策略
- 缺少慢调用（slow call）维度：成功但很慢的 provider 也应进入降级/熔断通道

**代码定位**：
```java
// FailoverService.java:149-158 - 所有错误都同等对待
.onErrorResume(error -> {
    scoreCalculator.update(state, false, duration);
    circuitBreaker.onFailure(state);
    return executeWithFailoverMono(...);  // 无差别 failover
})
```

### 1.5 评分与熔断耦合偏紧

**问题**：
- 熔断和 LB 评分混在一起，导致策略难以演进
- Failover 递归重试对"上游错误类型"缺少细粒度控制（例如：4xx/鉴权失败不应换 provider 盲重试）

### 1.6 递归 Failover 无限制

**问题**：
- 当前 Failover 实现是递归调用，没有 `maxFailoverAttempts` 限制
- 极端情况下可能导致请求链路过长、延迟飙升

**代码定位**：
```java
// FailoverService.java:158 - 递归无限制
return executeWithFailoverMono(callFunction, group, tried, timeoutMs);
```

### 1.7 性能与并发

**问题**：
- 最近窗口用 `Queue<Boolean>` + `stream()` 统计失败率，产生额外对象与 O(N) 遍历

**代码定位**：
```java
// ProviderScoreCalculator.java:107-109
Queue<Boolean> window = stats.getRecentResults();
long recentFailures = window.stream().filter(r -> !r).count();  // O(N) 遍历
```

### 1.8 流式与非流式处理差异

**现状**：
- 流式请求中途失败不能 Failover（已正确实现）
- 但流式首包失败的错误类型同样未分类

**代码定位**：
```java
// FailoverService.java:214-218 - 流式中途失败直接返回错误
if (firstChunk.get()) {
    // 首包失败可以 failover
} else {
    // 中途失败不能 failover
    return Flux.error(error);
}
```

### 1.9 多实例一致性风险

**问题**：
- 当前"落库+加载"更像是"持久化快照"
- 多个实例同时写同一行会互相覆盖，状态可能抖动
- 熔断状态是强时效数据；数据库落盘周期（10s）与网络故障恢复速度不匹配

---

## 2. 目标与原则

### 2.1 目标

| 目标 | 说明 |
|------|------|
| 稳定 | 快速隔离故障 Provider，避免级联失败与重试风暴 |
| 恢复快 | 健康恢复后能快速回流，且避免恢复期被瞬间打爆 |
| 可配置 | 不同模型组/不同 provider 可设置不同阈值 |
| 可观测 | 能解释"为何熔断/为何降权/何时恢复"，并支持报警 |
| 高性能 | 请求路径更新指标 O(1)、低分配、并发安全 |
| 可演进 | 支持本地单实例 → 多实例逐步升级 |

### 2.2 设计原则

1. **熔断与评分解耦**：熔断决定"能不能打"；评分决定"打谁更划算"
2. **明确的统计信号**：以错误率、慢调用率、连续失败、超时率触发熔断
3. **受控探测**：HALF_OPEN 必须限制探测并发和次数
4. **退避+抖动**：OPEN 时间采用指数退避，减少群体同步

---

## 3. 架构分层

整体拆成 4 层（从底到顶）：

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 4: 可观测与运维                                         │
│   metrics + event log + 管控开关                             │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: 路由与选择                                          │
│   LB（权重/评分/健康度）+ Failover 策略（有限重试）            │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: Provider 级熔断                                     │
│   Circuit Breaker（error rate + slow call + consecutive）   │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: 请求级保护                                          │
│   Timeout / Bulkhead（并发舱壁）/ RateLimit（可选）          │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Provider 级熔断（核心改造）

### 4.1 状态机

```
                    ┌──────────────────────────────────────┐
                    │                                      │
                    ▼                                      │
              ┌──────────┐                                │
              │  CLOSED  │──── 触发条件满足 ────────────────┼───┐
              └────┬─────┘                                │   │
                   │                                      │   │
                   │ 正常请求                             │   │
                   ▼                                      │   │
              ┌──────────┐                                │   │
              │   OPEN   │◄───────────────────────────────┘   │
              └────┬─────┘                                    │
                   │                                          │
                   │ 到达探测时间点                           │
                   ▼                                          │
              ┌──────────┐                                    │
              │HALF_OPEN │                                    │
              └────┬─────┘                                    │
                   │                                          │
         ┌─────────┼─────────┐                               │
         │                   │                               │
    探测成功≥阈值        探测失败≥阈值                       │
         │                   │                               │
         ▼                   ▼                               │
      CLOSED            OPEN (退避+1)                        │
         │                   │                               │
         └───────────────────┴───────────────────────────────┘
```

### 4.2 触发信号（同时支持 3 种，满足任意一种可 OPEN）

#### 4.2.1 错误率（Error Rate）
```java
触发条件：total >= minCalls && errorRate >= errorRateThreshold
```

#### 4.2.2 慢调用率（Slow Call Rate）
```java
slowCall 定义：latencyMs >= slowCallThresholdMs
触发条件：total >= minCalls && slowRate >= slowRateThreshold
```

#### 4.2.3 连续失败（Consecutive Failures）
```java
触发条件：consecutiveFailures >= consecutiveFailureThreshold
适用场景：完全不可用的瞬时隔离（鉴权失效、域名解析失败、持续超时）
```

### 4.3 HALF_OPEN 探测策略

**关键参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `permittedCallsInHalfOpen` | 2 | 半开允许的最大探测请求数 |
| `halfOpenSuccessThreshold` | 2 | 探测成功达到该次数 → CLOSED |
| `halfOpenFailureThreshold` | 1 | 探测失败达到该次数 → OPEN |
| `halfOpenMaxDurationMs` | 30000 | 半开最长持续时间 |

**行为**：
1. 进入 HALF_OPEN 时，初始化 `probeRemaining = permittedCallsInHalfOpen`
2. 每次 `allowRequest()`：`probeRemaining > 0` 则原子减 1 放行；否则拒绝
3. 探测成功/失败分别计数；达到阈值立即切换状态
4. HALF_OPEN 超时仍未决策：默认转 OPEN

### 4.4 失败类型分类

建议新增 `FailureType` 枚举：

```java
public enum FailureType {
    SUCCESS,           // 成功
    TIMEOUT,           // 超时（强信号，快速触发熔断）
    CONNECT,           // 连接失败（强信号）
    DNS,               // DNS 解析失败（强信号）
    TLS,               // TLS 握手失败（强信号）
    HTTP_5XX,          // 服务端错误（强信号）
    HTTP_429,          // 限流（中等信号，偏向退避而非立刻 OPEN）
    HTTP_4XX,          // 客户端错误（不计入熔断，不 failover）
    DECODE,            // 反序列化失败（可选计入）
    UNKNOWN            // 未知错误
}
```

**分类策略**：

| 类型 | 熔断权重 | 是否 Failover | 说明 |
|------|----------|---------------|------|
| TIMEOUT | 高 | 是 | 快速触发连续失败 |
| CONNECT/DNS/TLS | 高 | 是 | 通常持续性强 |
| HTTP_5XX | 高 | 是 | 服务端问题 |
| HTTP_429 | 中 | 是（可配） | 优先退避重试 |
| HTTP_4XX | 不计入 | 否 | 配置/请求问题，换 provider 无意义 |

### 4.5 OPEN 持续时间：指数退避 + 抖动

**参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `openBaseMs` | 5000 | 基础冷却时间 |
| `openMaxMs` | 300000 | 最大冷却时间（5分钟） |
| `openBackoffMultiplier` | 2.0 | 退避倍数 |
| `openJitterRatio` | 0.2 | 抖动比例（±20%） |

**算法**：
```java
// 每次从 HALF_OPEN 失败回到 OPEN 时 attempt++
long baseDuration = (long) (openBaseMs * Math.pow(openBackoffMultiplier, attempt));
long duration = Math.min(openMaxMs, baseDuration);
// 加抖动
double jitter = 1.0 + (random.nextDouble() * 2 - 1) * openJitterRatio;
long finalDuration = (long) (duration * jitter);
```

---

## 5. 高性能指标窗口

### 5.1 时间窗口 + 环形桶（Ring Buffer）

**替换现有 `Queue<Boolean>` 实现**，优势：
- 更新 O(1)，无对象分配
- 避免 `stream()` 遍历

**结构示例**：
```java
public class SlidingWindowMetrics {
    private final int bucketCount;        // 例如 10
    private final long bucketDurationMs;  // 例如 1000ms
    private final Bucket[] buckets;
    private volatile int currentIndex;

    static class Bucket {
        LongAdder total = new LongAdder();
        LongAdder errors = new LongAdder();
        LongAdder slow = new LongAdder();
        volatile long epoch;  // 用于懒清理
    }

    public void record(boolean success, boolean slow) {
        Bucket bucket = getCurrentBucket();  // 懒清理过期桶
        bucket.total.increment();
        if (!success) bucket.errors.increment();
        if (slow) bucket.slow.increment();
    }

    public double getErrorRate() {
        // 聚合所有有效桶
    }
}
```

### 5.2 连续失败计数

```java
private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

// 成功时清零
public void onSuccess() {
    consecutiveFailures.set(0);
}

// 失败时（仅对可重试失败类型）
public void onFailure(FailureType type) {
    if (type.isRetryable()) {
        consecutiveFailures.incrementAndGet();
    }
}
```

### 5.3 状态更新并发策略

```java
// 状态使用 AtomicReference + CAS
private final AtomicReference<CircuitState> state = new AtomicReference<>(CircuitState.CLOSED);

// HALF_OPEN 探测额度
private final AtomicInteger probeRemaining = new AtomicInteger(0);

// 状态切换需要 CAS 保证一致性
public boolean tryTransitionTo(CircuitState expected, CircuitState target) {
    return state.compareAndSet(expected, target);
}
```

---

## 6. Failover 策略改进

### 6.1 限制 Failover 次数

**新增参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `maxFailoverAttempts` | 3 | 最大尝试次数（含首次） |
| `maxTotalTimeMs` | `1.2 * timeoutMs` | 整体预算时间 |

**改造 FailoverService**：
```java
private Mono<ObjectNode> executeWithFailoverMono(
        Function<ModelGroupConfigItem, Mono<ObjectNode>> callFunction,
        ModelGroupConfig group,
        Set<String> tried,
        Integer timeoutMs,
        int attemptCount,    // 新增
        long startTimeMs     // 新增
) {
    // 检查重试次数
    if (attemptCount >= maxFailoverAttempts) {
        return Mono.error(new MaxFailoverExceededException(...));
    }

    // 检查总时间预算
    if (System.currentTimeMillis() - startTimeMs > maxTotalTimeMs) {
        return Mono.error(new TimeoutException("Total failover budget exceeded"));
    }

    // ... 原有逻辑
}
```

### 6.2 按错误类型决定是否 Failover

```java
.onErrorResume(error -> {
    FailureType type = classifyError(error);
    scoreCalculator.update(state, type, duration);
    circuitBreaker.onFailure(state, type);

    if (!type.shouldFailover()) {
        // 4xx 等不应该 failover 的错误直接返回
        return Mono.error(error);
    }

    return executeWithFailoverMono(..., attemptCount + 1, startTimeMs);
});
```

### 6.3 错误分类实现

```java
private FailureType classifyError(Throwable error) {
    if (error instanceof TimeoutException) {
        return FailureType.TIMEOUT;
    }
    if (error instanceof ConnectException) {
        return FailureType.CONNECT;
    }
    if (error instanceof WebClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 429) return FailureType.HTTP_429;
        if (status >= 500) return FailureType.HTTP_5XX;
        if (status >= 400) return FailureType.HTTP_4XX;
    }
    // ... 其他分类
    return FailureType.UNKNOWN;
}
```

---

## 7. Bulkhead（并发舱壁）

### 7.1 每 Provider 并发上限

**参数**：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `maxConcurrentRequestsPerProvider` | 50 | 每 Provider 最大并发数 |

**实现**：
```java
public class ProviderBulkhead {
    private final AtomicInteger currentConcurrent = new AtomicInteger(0);
    private final int maxConcurrent;

    public boolean tryAcquire() {
        while (true) {
            int current = currentConcurrent.get();
            if (current >= maxConcurrent) {
                return false;  // 快速失败
            }
            if (currentConcurrent.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    public void release() {
        currentConcurrent.decrementAndGet();
    }
}
```

**在 FailoverService 中使用**：
```java
Mono<ObjectNode> result = Mono.defer(() -> {
    if (!state.getBulkhead().tryAcquire()) {
        return Mono.error(new BulkheadFullException(providerId));
    }
    return callFunction.apply(item);
})
.doFinally(signal -> state.getBulkhead().release());
```

---

## 8. 负载均衡（LB）与熔断解耦

### 8.1 分离职责

| 组件 | 职责 |
|------|------|
| CircuitBreaker | 决定 Provider 是否可用（OPEN = 不可选） |
| ScoreCalculator | 计算 Provider 健康评分（用于排序/选择） |
| FailoverService | 基于熔断状态和评分选择 Provider |

### 8.2 选择流程

```java
public ModelGroupConfigItem selectAvailableProvider(...) {
    // 1. 筛选可用 Provider
    List<ModelGroupConfigItem> eligible = items.stream()
        .filter(item -> {
            ProviderRuntimeState stats = registry.get(generateProviderId(item));
            CircuitState state = stats.getCircuitState();

            // OPEN 状态不可选（除非到达探测时间）
            if (state == CircuitState.OPEN) {
                return circuitBreaker.shouldTransitionToHalfOpen(stats);
            }
            // HALF_OPEN 需要检查探测额度
            if (state == CircuitState.HALF_OPEN) {
                return stats.hasProbeQuota();
            }
            return true;  // CLOSED 可选
        })
        .toList();

    // 2. 对 HALF_OPEN 加惩罚系数
    // 3. Top-K + Softmax 选择（保持现有逻辑）
}
```

### 8.3 HALF_OPEN 惩罚系数

```java
double score = stats.getScore();
if (stats.getCircuitState() == CircuitState.HALF_OPEN) {
    score *= 0.5;  // 恢复期降权，避免流量过大
}
```

---

## 9. 持久化与多实例策略

### 9.1 单实例推荐做法

| 方面 | 建议 |
|------|------|
| 状态存储 | 内存为主，落库只做观测与重启恢复 |
| 落库频率 | 可降低至 30s~60s，减少 DB 压力 |
| 实时决策 | 不依赖 DB（时效性太差） |

### 9.2 多实例策略（未来可选）

**路线 A：每实例独立熔断（推荐先实现）**
- 各实例按自身观测熔断
- 优点：实现简单，无一致性开销
- 缺点：同一 provider 在不同实例可能状态不同

**路线 B：共享熔断状态（复杂，按需实现）**
- 使用 Redis 存储熔断状态机
- 指标窗口仍在本地（高频更新）
- 需要分布式锁或 Lua CAS

**建议**：先完善本地熔断，验证路线 A 是否满足需求后再考虑路线 B。

---

## 10. 可观测性与运维管控

### 10.1 Metrics（Prometheus/Actuator）

每 Provider 输出：
```yaml
# 熔断状态 (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
lumina_circuit_state{provider_id="xxx"} 0

# 错误率与慢调用率
lumina_error_rate{provider_id="xxx"} 0.05
lumina_slow_rate{provider_id="xxx"} 0.10

# 连续失败次数
lumina_consecutive_failures{provider_id="xxx"} 0

# 并发使用量
lumina_bulkhead_concurrent{provider_id="xxx"} 10
lumina_bulkhead_rejected_total{provider_id="xxx"} 0

# 调用耗时分位数
lumina_request_duration_seconds{provider_id="xxx", quantile="0.5"} 0.8
lumina_request_duration_seconds{provider_id="xxx", quantile="0.95"} 2.5
lumina_request_duration_seconds{provider_id="xxx", quantile="0.99"} 5.0
```

### 10.2 Event Log（结构化日志）

每次状态切换输出：
```json
{
  "event": "circuit_state_change",
  "providerId": "xxx",
  "providerName": "openai-gpt4",
  "fromState": "CLOSED",
  "toState": "OPEN",
  "reason": "error_rate_threshold",
  "errorRate": 0.65,
  "slowRate": 0.30,
  "consecutiveFailures": 5,
  "openDurationMs": 5000,
  "attempt": 1,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 10.3 管控开关

| 操作 | 说明 |
|------|------|
| 手动 OPEN | 强制隔离某 Provider |
| 手动 CLOSE | 强制恢复某 Provider |
| 临时降权 | 减少流量但不完全熔断 |
| 动态配置 | 运行时调整阈值 |

---

## 11. 配置体系设计

### 11.1 全局默认配置

```yaml
lumina:
  circuit-breaker:
    # 触发条件
    min-calls: 20
    error-rate-threshold: 0.5
    slow-call-threshold-ms: 4000
    slow-rate-threshold: 0.6
    consecutive-failure-threshold: 5

    # HALF_OPEN
    permitted-calls-in-half-open: 2
    half-open-success-threshold: 2
    half-open-failure-threshold: 1
    half-open-max-duration-ms: 30000

    # OPEN 退避
    open-base-ms: 5000
    open-max-ms: 300000
    open-backoff-multiplier: 2.0
    open-jitter-ratio: 0.2

    # Bulkhead
    max-concurrent-requests-per-provider: 50

    # Failover
    max-failover-attempts: 3
```

### 11.2 按 Group/Provider 覆盖（可选）

```yaml
lumina:
  model-groups:
    gpt-4:
      circuit-breaker:
        error-rate-threshold: 0.3  # 对高价值模型更敏感
        max-concurrent-requests-per-provider: 100
```

---

## 12. 推荐默认参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `minCalls` | 20 | 最少请求数（现有 15，建议提高） |
| `errorRateThreshold` | 50% | 错误率阈值 |
| `slowCallThresholdMs` | 4000 | 慢调用定义 |
| `slowRateThreshold` | 60% | 慢调用率阈值 |
| `consecutiveFailureThreshold` | 5 | 连续失败阈值 |
| `permittedCallsInHalfOpen` | 2 | 半开探测数 |
| `halfOpenSuccessThreshold` | 2 | 半开成功阈值 |
| `halfOpenFailureThreshold` | 1 | 半开失败阈值 |
| `halfOpenMaxDurationMs` | 30000 | 半开超时 |
| `openBaseMs` | 5000 | OPEN 基础时间 |
| `openMaxMs` | 300000 | OPEN 最大时间 |
| `openBackoffMultiplier` | 2.0 | 退避倍数 |
| `openJitterRatio` | 0.2 | 抖动比例 |
| `maxConcurrentRequestsPerProvider` | 50 | Provider 并发上限 |
| `maxFailoverAttempts` | 3 | 最大重试次数 |

---

## 13. 代码改造映射表

| 现有文件 | 改造内容 |
|----------|----------|
| `CircuitBreaker.java` | 重构为完整状态机，支持受控探测、退避、分类 |
| `CircuitState.java` | 保持不变 |
| `ProviderRuntimeState.java` | 新增字段：`probeRemaining`, `openAttempt`, `nextProbeAt`, `consecutiveFailures`, `bulkhead` |
| `ProviderScoreCalculator.java` | 替换 Queue 为 RingBuffer，支持 FailureType |
| `FailoverService.java` | 添加重试限制、错误分类、Bulkhead 检查 |
| `ProviderStatsPersistenceJob.java` | 适配新字段 |
| 新增 `FailureType.java` | 错误类型枚举 |
| 新增 `SlidingWindowMetrics.java` | 环形桶实现 |
| 新增 `ProviderBulkhead.java` | 并发舱壁 |
| 新增 `CircuitBreakerConfig.java` | 配置类 |

---

## 14. 落地路径（分 3 阶段）

### 阶段 1（最优先，收益最大）

- [ ] HALF_OPEN 受控探测（限制探测请求数/并发）
- [ ] 错误类型分类 + Failover 决策（4xx 不盲切）
- [ ] OPEN 指数退避 + jitter
- [ ] consecutiveFailures 独立计数（不依赖 score）
- [ ] Failover 最大次数限制

### 阶段 2（性能与稳定性增强）

- [ ] 采用时间窗口 ring buffer 替换 Queue/stream
- [ ] Bulkhead（每 provider 并发上限）
- [ ] 指标/事件完善（状态切换可解释）
- [ ] Prometheus metrics 输出

### 阶段 3（可选进阶）

- [ ] per group/provider 动态配置
- [ ] 管控接口（手动 OPEN/CLOSE）
- [ ] 多实例共享熔断状态（如确有必要）

---

## 15. 风险与权衡

| 风险 | 缓解措施 |
|------|----------|
| 参数过多难以调优 | 先固定默认值，逐步放开可配 |
| HALF_OPEN 过于保守导致恢复慢 | 从 `permittedCallsInHalfOpen=2` 起步，观察调整 |
| 错误分类不准确 | 先做主要类型（Timeout/5xx/4xx），逐步完善 |
| 多实例共享状态复杂 | 优先验证独立熔断是否满足需求 |
| Ring Buffer 实现复杂 | 可先用简化版本，验证后再优化 |

---

## 16. 状态判定伪代码

```text
onRequestStart(provider):
  if state == OPEN:
    if now < nextProbeAt: reject
    else if CAS(OPEN -> HALF_OPEN): initHalfOpen()

  if state == HALF_OPEN:
    if probeRemaining.decrementAndGet() < 0: reject

  if bulkhead.tryAcquire() == false: reject

  allow

onRequestResult(provider, result):
  classify result -> (success, failureType, latencyMs, isSlow)

  updateWindowCounters(success, failureType, isSlow)

  if success:
    consecutiveFailures = 0
  else if failureType.isRetryable():
    consecutiveFailures++

  switch state:
    CLOSED:
      if shouldOpen():
        OPEN(attempt=0, duration=openBaseMs+jitter)

    HALF_OPEN:
      if success: halfOpenSuccessCount++
      else: halfOpenFailureCount++

      if halfOpenFailureCount >= threshold:
        OPEN(attempt++, duration=backoff(attempt)+jitter)
      if halfOpenSuccessCount >= threshold:
        CLOSE(resetWindow)
```

---

## 17. 结论

最推荐的升级组合是：

> **"分类 + 受控半开探测 + 指数退避抖动 + 并发舱壁 + Failover 限制 + 统一指标/事件"**

它在工程复杂度、稳定性收益、高性能之间取得较好平衡，且与现有 Failover/LB 结构兼容。

核心改造路径：
1. 将熔断独立成"纯健康判定模块"
2. 让 `FailoverService` 使用它做筛选与探测
3. 逐步添加可观测性和管控能力

通过分阶段实施，可以在保证系统稳定的前提下，逐步提升容错能力。
