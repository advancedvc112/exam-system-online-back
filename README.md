# Exam System Online - 项目说明

## 1. 项目结构

- `exam-system-online-server`：Spring Boot 启动与配置层，读取 `application.yml`（MySQL/Redis/RocketMQ/定时任务）。
- `exam-system-online-core`：核心业务
  - `config`：Redis 序列化、RocketMQ 占位配置。
  - `controller`：`auth`、`questions`、`exams`、`student/exams`。
  - `service`：认证、题库、考试管理、学生进入考试、答题缓冲与存储、定时任务。
  - `dal`：数据对象与 MyBatis-Plus Mapper。
  - `consumer`：RocketMQ 答题落库。
  - `scheduler`：定时切换考试状态。

## 2. 业务模块

- 认证与用户：注册/登录（BCrypt），登录更新 `last_login_time`，角色 1=学生、2=教师、3=管理员。
- 题库：按标签/分页查询；创建与更新题目（单选/多选/判断等）。
- 考试与组卷：创建、分页查询、更新（时间/状态/题目替换）、批量添加题目、自动计算总分；定时任务按开始时间自动置为进行中。
- 学生考试：进入考试发 token，保存答题（3 秒缓冲、写 Redis + 发 MQ），提交考试（刷新缓冲、标记提交）。

## 3. 接口与路径

- 认证 `/api/auth`
  - `POST /login` 登录（不存在则自动创建学生）。
  - `POST /register` 注册（可指定 `userRole`）。
- 题目 `/api/questions`
  - `GET /search-by-tag?tag&page&size` 按标签。
  - `GET /list?page&size` 全量分页。
  - `POST /create` 创建题目。
  - `PUT /update` 更新题目。
- 考试管理 `/api/exams`
  - `GET /list?page&size` 考试列表。
  - `POST /create` 创建考试。
  - `POST /{examId}/questions` 批量添加题目。
  - `PUT /{examId}` 更新考试信息（可替换题目映射）。
  - `PUT /{examId}/status` 修改状态/时间（仅教师/管理员）。
- 学生考试 `/api/student/exams`
  - `POST /{examId}/enter` 进入考试，返回 token 和 participantId。
  - `POST /{examId}/answers` 保存/修改单题答案（缓冲写）。
  - `POST /{examId}/submit` 提交考试（刷新缓冲并标记提交）。

## 4. 核心数据表

- `system_users`：用户/角色/登录信息。
- `questions_bank`：题库。
- `exams`：考试信息（时间、状态、总分、题量）。
- `exam_questions`：试卷题目编排（分数、序号、分组）。
- `exam_participants`：学生进入/提交状态、token。
- `answer_records`：答题记录、修改次数、得分、评审状态。

## 5. 高并发处理（进入考试与修改答案）

### 5.1 进入考试（发放 token + 分布式锁优化）

- **分布式锁机制**：
  - 使用 Redisson 分布式锁，锁 key：`exam:enter:lock:{examId}:{studentId}`
  - 锁超时时间：30 秒（自动续期 watchdog）
  - 获取锁失败策略：等待 100ms 后查询 Redis 中现有 token，如果存在则返回，否则返回"系统繁忙"
- **流程**：
  1. 尝试获取分布式锁（不等待，立即返回）
  2. 获取锁成功：校验学生身份与考试时间 → 检查是否已进入（幂等）→ 生成 token → Redis `SETNX` 写 `exam:token:{examId}:{studentId}`（TTL=考试结束+30 分钟）→ upsert `exam_participants` 记录 → 释放锁
  3. 获取锁失败：等待 100ms → 查询 Redis 中现有 token → 如果存在则返回参与记录，否则返回错误
- **关键点**：
  - 分布式锁保证同一学生并发请求的串行化，防止重复进入
  - SETNX 原子操作双重保障，防止重复写入 token
  - TTL 避免长期占用；幂等 upsert；进入失败直接抛出，不影响其他人

### 5.2 答题保存/修改（3 秒缓冲 + Redis + MQ）

- 写入侧（StudentExamController → AnswerBufferService）：
  - 将最新答案放入内存 buffer（key=examId:studentId:sortOrder），取消旧定时任务，重新排一个 3 秒后执行的任务。
  - 3 秒窗口内多次修改同题，只保留最后一次，削峰减少 Redis/MQ 压力。
- 刷新侧（AnswerBufferService.flushAnswer）：
  - 将答案写入 Redis `exam:ans:{examId}:{studentId}:{sortOrder}`，TTL 60s（每次更新续期）。
  - 读取 `exam_questions` 获取 questionId，封装 `AnswerMessage` 发送 RocketMQ topic `exam-answer-save`。
- 落库侧（AnswerRecordConsumer）：
  - 按 participantId+questionId upsert `answer_records`，更新 `userAnswer` 与 `changeTimes`，记录分值；RocketMQ 自带重试/死信保证最终一致。
- 一致性：短期以内存+Redis+MQ+DB 多副本，最终以 DB 为准；Redis 作为近期答案热缓存；MQ 失败可重试或进死信。

### 5.3 提交考试（分布式锁优化）

- **分布式锁机制**：
  - 使用 Redisson 分布式锁，锁 key：`exam:submit:lock:{examId}:{studentId}`
  - 锁超时时间：10 秒（自动续期 watchdog）
  - 获取锁失败策略：查询当前状态，如果已提交则返回成功，否则返回"提交中，请勿重复提交"
- **流程**（在锁内执行）：
  1. 尝试获取分布式锁（不等待，立即返回）
  2. 获取锁成功：
     - 检查是否已提交（幂等检查）
     - 调用 `flushAll(examId, studentId)`：同步冲刷 buffer 中该考生所有题目，取消未到期任务
     - 等待 500ms 让 MQ 发送完成（生产可用回调/积压监控替代）
     - 将 `exam_participants` 状态置为已提交（幂等）
     - 释放锁
  3. 获取锁失败：查询当前状态，如果已提交则返回成功，否则返回错误
- **关键点**：
  - 分布式锁保证同一学生并发提交请求的串行化，防止重复提交
  - flushAll 和状态更新在锁内执行，保证原子性
  - 幂等处理，已提交的请求直接返回成功

### 5.4 失败与补偿

- Redis 写失败：抛出异常，前端可重试；可增加降级直接发 MQ。
- MQ 发送失败：记录日志，可选本地/Redis 重试队列；消费失败 RocketMQ 自动重试，超限进死信。
- 数据补偿：提交时或定期任务扫描 Redis `exam:ans:*`，对缺失的 DB 记录进行补写。

### 5.5 扩展与限流

- 横向扩展：提升 AnswerBuffer 线程池，或将缓冲合并迁到队列分片；MQ 分区/多 topic 分摊写入。
- 限流与保护：对进入考试/答题接口可加学生-考试级别的速率限制；Redis key TTL 自动清理长时间不活跃的会话。

### 5.6 用户级别限流（应用层）

- **限流实现**：
  - 使用 AOP 切面 + Redis + Lua 脚本实现分布式限流
  - 限流算法：令牌桶算法（允许突发流量）
  - 限流维度：用户级别（基于 studentId）
- **限流规则**：
  - 进入考试：每个学生每秒最多 2 次
  - 保存答题：每个学生每秒最多 10 次
  - 提交考试：每个学生每秒最多 1 次
- **限流流程**：
  1. Controller 方法上标注 `@UserRateLimit` 注解
  2. AOP 切面拦截方法执行，提取用户ID
  3. 调用限流服务检查是否允许通过
  4. 使用 Redis + Lua 脚本执行令牌桶算法
  5. 如果被限流，抛出 `RateLimitException`，返回 429 状态码
- **降级策略**：
  - Redis 故障时：允许通过（fail-open），记录告警
  - 限流服务异常时：允许通过，记录告警
- **监控统计**：
  - 记录限流成功/失败次数、失败率
  - 日志格式：`[限流监控]` 前缀，便于日志分析和过滤

### 5.7 监控与预警

- **锁监控**：
  - 实时记录锁的获取、释放、持有时间、竞争情况
  - 统计锁的成功/失败次数、平均持有时间、失败率
  - 定时输出锁监控统计信息（每5分钟）
  - 自动告警：失败率 > 10% 或平均持有时间过长时记录警告日志
  - 日志格式：`[锁监控]` 前缀，便于日志分析和过滤
- **限流监控**：
  - 记录限流触发次数、失败率
  - 日志格式：`[限流监控]` 前缀，便于日志分析和过滤
- **其他关键指标**：SETNX 失败率（重复进入）、Redis 写/读失败率、MQ 发送/消费延迟与积压、答题落库成功率、buffer 队列长度。
- **日志**：进入考试、刷写答案、提交考试都记录 examId/studentId/sortOrder 以便追溯。

## 6. 配置与运行

- 配置文件：`exam-system-online-server/src/main/resources/application.yml`
  - MySQL: `jdbc:mysql://localhost:3306/online_exam_system`
  - Redis: `host/port/db` (默认 0)
  - Redisson: 自动配置，连接池大小 10，最小空闲连接 5
  - RocketMQ: `name-server`, topic `exam-answer-save`
  - 定时任务：`exam.status.update-interval`、`initial-delay`
  - 限流配置：`rate-limit.user-level.rules`（进入考试、保存答题、提交考试的限流规则）
- 依赖说明：
  - Redisson 3.24.3：用于分布式锁（已添加到 `exam-system-online-core/pom.xml`）
  - Spring Boot Starter AOP：用于限流切面（已添加到 `exam-system-online-core/pom.xml`）
- 启动顺序：启动 MySQL、Redis、RocketMQ → 运行 `exam-system-online-server`。
- 初始化：导入 `online_exam_system.sql`。

## 7. 测试与压测

- JMeter 脚本位于 `test/`（如 `exam-test-fixed.jmx`），可对进入考试、答题保存与提交进行并发压测。

## 8. 监控模块（exam-system-online-actuator）

该项目提供独立的监控模块 `exam-system-online-actuator`，基于 Spring Boot Actuator + Micrometer + Prometheus + Grafana，便于在生产环境观察应用健康与关键业务指标。已把模块文档合并到此处，便于集中查看。

### 功能特性

- ✅ 接口监控：HTTP 请求 QPS、延迟、错误率
- ✅ 数据库监控：连接池状态、SQL 执行时间
- ✅ Redis 监控：连接数、命令执行时间、内存使用
- ✅ RocketMQ 监控：消息发送/消费速率、延迟、积压
- ✅ 业务监控：限流统计、分布式锁统计
- ✅ JVM 监控：内存、GC、线程

### 模块结构（代码位置）

```text
exam-system-online-actuator/
├── src/main/java/com/exam/online/actuator/
│   ├── ActuatorApplication.java          # 启动类
   │   ├── config/
   │   │   └── ActuatorConfig.java        # Actuator 配置
   │   ├── metrics/
   │   │   └── CustomMetricsCollector.java# 自定义指标收集器
   │   └── health/
   │       └── CustomHealthIndicator.java # 自定义健康检查
├── src/main/resources/
│   └── application.yml                    # 配置文件
├── prometheus.yml                         # Prometheus 配置示例
├── grafana-dashboard.json                 # Grafana 仪表板配置
└── README.md                              # 已合并到根 README
```

### 快速开始（本地/容器）

- 启动监控模块（开发）

```bash
cd exam-system-online-actuator
mvn spring-boot:run
```

- 打包后启动

```bash
mvn clean package
java -jar target/exam-system-online-actuator-1.0-SNAPSHOT.jar
```

监控模块默认运行在 `http://localhost:8081`（可通过 `application.yml` 修改）。

### 常用端点

- 健康检查: `http://localhost:8081/actuator/health`
- Prometheus 指标: `http://localhost:8081/actuator/prometheus`
- 所有指标: `http://localhost:8081/actuator/metrics`
- 应用信息: `http://localhost:8081/actuator/info`

### 配置 Prometheus（示例）

1. 将项目根目录下的 `prometheus.yml`（actuator 模块内）复制到 Prometheus 安装目录；
2. 在 `prometheus.yml` 的 `scrape_configs.targets` 中加入 `localhost:8081` 或实际地址；
3. 启动 Prometheus：

```bash
./prometheus --config.file=prometheus.yml
```

也可以使用 Docker 方式运行 Prometheus 与 Grafana（略）。

### 监控指标说明（业务与系统）

部分业务指标示例：

- `exam_rate_limit_success_total`：限流成功次数（Counter）
- `exam_rate_limit_failure_total`：限流失败次数（Counter）
- `exam_rate_limit_failure_rate`：限流失败率（Gauge）
- `exam_lock_submit_exam_hold_time_seconds`：提交考试锁持有时间（Timer）

系统指标（Micrometer/Actuator）示例：

- `http_server_requests_seconds`：HTTP 请求耗时
- `jvm_memory_used_bytes`：JVM 内存使用
- `jdbc_connections_active`：数据库活跃连接数

### 安全建议（生产）

在生产环境请缩小暴露的 actuator 端点，例如：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true
```

并在反向代理/网关层做鉴权与白名单限制。

### 扩展与告警

- 可在 `CustomMetricsCollector` 中注册自定义业务指标。
- 在 Prometheus 中添加 `alert_rules.yml`，配置告警规则（如错误率、MQ 积压告警）。

### 参考资料

- Spring Boot Actuator 文档: [https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- Micrometer 文档: [https://micrometer.io/docs](https://micrometer.io/docs)
- Prometheus 文档: [https://prometheus.io/docs/](https://prometheus.io/docs/)
- Grafana 文档: [https://grafana.com/docs/](https://grafana.com/docs/)

## 9. 许可证

MIT