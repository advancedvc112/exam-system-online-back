# 考试系统监控模块

基于 Spring Boot Actuator + Micrometer + Prometheus + Grafana 的监控解决方案。

## 功能特性

- ✅ **接口监控**：HTTP 请求 QPS、延迟、错误率
- ✅ **数据库监控**：连接池状态、SQL 执行时间
- ✅ **Redis 监控**：连接数、命令执行时间、内存使用
- ✅ **RocketMQ 监控**：消息发送/消费速率、延迟、积压
- ✅ **业务监控**：限流统计、分布式锁统计
- ✅ **JVM 监控**：内存、GC、线程

## 模块结构

```
exam-system-online-actuator/
├── src/main/java/com/exam/online/actuator/
│   ├── ActuatorApplication.java          # 启动类
│   ├── config/
│   │   └── ActuatorConfig.java           # Actuator 配置
│   ├── metrics/
│   │   └── CustomMetricsCollector.java   # 自定义指标收集器
│   └── health/
│       └── CustomHealthIndicator.java    # 自定义健康检查
├── src/main/resources/
│   └── application.yml                    # 配置文件
├── prometheus.yml                         # Prometheus 配置示例
├── grafana-dashboard.json                 # Grafana 仪表板配置
└── README.md                              # 本文档
```

## 快速开始

### 1. 启动监控模块

```bash
# 方式一：Maven 启动
cd exam-system-online-actuator
mvn spring-boot:run

# 方式二：打包后启动
mvn clean package
java -jar target/exam-system-online-actuator-1.0-SNAPSHOT.jar
```

监控模块默认运行在 `http://localhost:8081`

### 2. 访问监控端点

- **健康检查**: http://localhost:8081/actuator/health
- **Prometheus 指标**: http://localhost:8081/actuator/prometheus
- **所有指标**: http://localhost:8081/actuator/metrics
- **应用信息**: http://localhost:8081/actuator/info

### 3. 配置 Prometheus

#### 方式一：直接安装 Prometheus

1. 下载 Prometheus: https://prometheus.io/download/
2. 将 `prometheus.yml` 复制到 Prometheus 安装目录
3. 修改 `prometheus.yml` 中的 `targets` 为实际的应用地址
4. 启动 Prometheus:
   ```bash
   ./prometheus --config.file=prometheus.yml
   ```
5. 访问 Prometheus UI: http://localhost:9090

#### 方式二：使用 Docker

```bash
docker run -d \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### 4. 配置 Grafana

#### 方式一：直接安装 Grafana

1. 下载 Grafana: https://grafana.com/grafana/download
2. 启动 Grafana:
   ```bash
   ./grafana-server
   ```
3. 访问 Grafana UI: http://localhost:3000（默认账号/密码: admin/admin）
4. 添加 Prometheus 数据源:
   - 进入 Configuration → Data Sources
   - 点击 Add data source
   - 选择 Prometheus
   - URL: http://localhost:9090
   - 点击 Save & Test
5. 导入仪表板:
   - 进入 Dashboards → Import
   - 上传 `grafana-dashboard.json` 文件
   - 或使用仪表板 ID（如果有）

#### 方式二：使用 Docker

```bash
docker run -d \
  -p 3000:3000 \
  grafana/grafana
```

## 监控指标说明

### 业务指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `exam_rate_limit_success_total` | Counter | 限流成功次数 |
| `exam_rate_limit_failure_total` | Counter | 限流失败次数 |
| `exam_rate_limit_failure_rate` | Gauge | 限流失败率（百分比） |
| `exam_lock_enter_exam_success_total` | Counter | 进入考试锁获取成功次数 |
| `exam_lock_enter_exam_failure_total` | Counter | 进入考试锁获取失败次数 |
| `exam_lock_enter_exam_failure_rate` | Gauge | 进入考试锁失败率 |
| `exam_lock_enter_exam_hold_time_seconds` | Timer | 进入考试锁持有时间 |
| `exam_lock_submit_exam_success_total` | Counter | 提交考试锁获取成功次数 |
| `exam_lock_submit_exam_failure_total` | Counter | 提交考试锁获取失败次数 |
| `exam_lock_submit_exam_failure_rate` | Gauge | 提交考试锁失败率 |
| `exam_lock_submit_exam_hold_time_seconds` | Timer | 提交考试锁持有时间 |

### 系统指标

| 指标名称 | 说明 |
|---------|------|
| `http_server_requests_seconds` | HTTP 请求耗时 |
| `jvm_memory_used_bytes` | JVM 内存使用 |
| `jvm_gc_pause_seconds` | GC 暂停时间 |
| `jdbc_connections_active` | 数据库活跃连接数 |
| `redis_command_duration_seconds` | Redis 命令执行时间 |

## 配置说明

### application.yml 主要配置项

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # 暴露所有端点
  endpoint:
    health:
      show-details: always  # 显示健康检查详情
  metrics:
    export:
      prometheus:
        enabled: true  # 启用 Prometheus 导出
        step: 30s      # 抓取间隔
```

### 安全配置（生产环境建议）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 只暴露必要的端点
  endpoint:
    health:
      show-details: when-authorized  # 仅授权用户可见详情
  security:
    enabled: true  # 启用安全认证
```

## 常见问题

### 1. 无法访问监控端点

- 检查端口是否被占用
- 检查 `management.endpoints.web.exposure.include` 配置
- 检查防火墙设置

### 2. Prometheus 无法抓取指标

- 检查 Prometheus 配置中的 `targets` 地址是否正确
- 检查应用是否正常运行
- 检查网络连接

### 3. Grafana 无法显示数据

- 检查 Prometheus 数据源配置是否正确
- 检查 Prometheus 是否正常运行
- 检查指标名称是否正确（区分大小写）

## 扩展功能

### 添加自定义指标

在 `CustomMetricsCollector` 中添加新的指标：

```java
private Counter customCounter;

@PostConstruct
public void init() {
    customCounter = Counter.builder("exam.custom.metric")
        .description("自定义指标")
        .register(meterRegistry);
}
```

### 添加告警规则

在 Prometheus 配置文件中添加告警规则：

```yaml
rule_files:
  - "alert_rules.yml"
```

创建 `alert_rules.yml`:

```yaml
groups:
  - name: exam_system_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        annotations:
          summary: "错误率过高"
```

## 参考资料

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer 文档](https://micrometer.io/docs)
- [Prometheus 文档](https://prometheus.io/docs/)
- [Grafana 文档](https://grafana.com/docs/)

## 许可证

MIT

