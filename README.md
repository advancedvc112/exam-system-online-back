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

### 5.1 进入考试（发放 token）

- 流程：校验学生身份与考试时间 → 生成 token → Redis `SETNX` 写 `exam:token:{examId}:{studentId}`（TTL=考试结束+30 分钟）→ 已存在直接拒绝，防重复进入与并发踩踏 → upsert `exam_participants` 记录（状态=进行中，写 token）。
- 关键点：SETNX 原子；TTL 避免长期占用；幂等 upsert；进入失败直接抛出，不影响其他人。

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

### 5.3 提交考试

- Controller 先调用 `flushAll(examId, studentId)`：同步冲刷 buffer 中该考生所有题目，取消未到期任务。
- 等待 500ms 让 MQ 发送完成（生产可用回调/积压监控替代），再将 `exam_participants` 状态置为已提交（幂等）。
- 如需要可增加：提交前从 Redis pattern 扫描补写一次 DB，减少极端丢失窗口。

### 5.4 失败与补偿

- Redis 写失败：抛出异常，前端可重试；可增加降级直接发 MQ。
- MQ 发送失败：记录日志，可选本地/Redis 重试队列；消费失败 RocketMQ 自动重试，超限进死信。
- 数据补偿：提交时或定期任务扫描 Redis `exam:ans:*`，对缺失的 DB 记录进行补写。

### 5.5 扩展与限流

- 横向扩展：提升 AnswerBuffer 线程池，或将缓冲合并迁到队列分片；MQ 分区/多 topic 分摊写入。
- 限流与保护：对进入考试/答题接口可加学生-考试级别的速率限制；Redis key TTL 自动清理长时间不活跃的会话。

### 5.6 监控与预警

- 关键指标：SETNX 失败率（重复进入）、Redis 写/读失败率、MQ 发送/消费延迟与积压、答题落库成功率、buffer 队列长度。
- 日志：进入考试、刷写答案、提交考试都记录 examId/studentId/sortOrder 以便追溯。

## 6. 配置与运行

- 配置文件：`exam-system-online-server/src/main/resources/application.yml`
  - MySQL: `jdbc:mysql://localhost:3306/online_exam_system`
  - Redis: `host/port/db` (默认 0)
  - RocketMQ: `name-server`, topic `exam-answer-save`
  - 定时任务：`exam.status.update-interval`、`initial-delay`
- 启动顺序：启动 MySQL、Redis、RocketMQ → 运行 `exam-system-online-server`。
- 初始化：导入 `online_exam_system.sql`。

## 7. 测试与压测

- JMeter 脚本位于 `test/`（如 `exam-test-fixed.jmx`），可对进入考试、答题保存与提交进行并发压测。
