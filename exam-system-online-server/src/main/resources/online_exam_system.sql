/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44-log)
 Source Host           : localhost:3306
 Source Schema         : online_exam_system

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44-log)
 File Encoding         : 65001

 Date: 10/12/2025 14:56:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for answer_records
-- ----------------------------
DROP TABLE IF EXISTS `answer_records`;
CREATE TABLE `answer_records`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '答题记录id',
  `participant_id` bigint(20) NOT NULL COMMENT '考试参与记录id',
  `exam_id` bigint(20) NOT NULL COMMENT '考试id',
  `question_id` bigint(20) NOT NULL COMMENT '题目id',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '用户答案',
  `change_times` int(11) NULL DEFAULT 0 COMMENT '用户更改次数',
  `correct_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '正确答案',
  `is_correct` tinyint(4) NULL DEFAULT 0 COMMENT '是否答对',
  `question_score` int(11) NOT NULL COMMENT '本题分值',
  `true_score` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '本题实际得分',
  `review_status` tinyint(4) NULL DEFAULT 0 COMMENT '批阅状态',
  `reviewer_id` bigint(20) NULL DEFAULT NULL COMMENT '批阅者id',
  `review_time` datetime NULL DEFAULT NULL COMMENT '批阅时间',
  `created_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_participant_question`(`participant_id`, `question_id`) USING BTREE,
  INDEX `idx_participant_id`(`participant_id`) USING BTREE,
  INDEX `idx_exam_question`(`exam_id`, `question_id`) USING BTREE,
  INDEX `idx_review_status`(`review_status`) USING BTREE,
  INDEX `idx_is_correct`(`is_correct`) USING BTREE,
  INDEX `fk_answer_records_question`(`question_id`) USING BTREE,
  CONSTRAINT `fk_answer_records_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_answer_records_participant` FOREIGN KEY (`participant_id`) REFERENCES `exam_participants` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_answer_records_question` FOREIGN KEY (`question_id`) REFERENCES `questions_bank` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '答题记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of answer_records
-- ----------------------------

-- ----------------------------
-- Table structure for exam_participants
-- ----------------------------
DROP TABLE IF EXISTS `exam_participants`;
CREATE TABLE `exam_participants`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '参与记录id',
  `exam_id` bigint(20) NOT NULL COMMENT '考试id',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `attempt` int(11) NULL DEFAULT 1 COMMENT '第几次尝试进入',
  `join_time` datetime NULL DEFAULT NULL COMMENT '考试进入时间',
  `start_time` datetime NULL DEFAULT NULL COMMENT '答题开始时间',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '考试提交时间',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '考试状态（0：未开始 1：进行中 2：已提交 3：超时 4：强制交卷 5：异常断开 6：已过期）',
  `access_token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '当前访问令牌',
  `connection_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'webSocket连接id',
  `is_connected` tinyint(4) NULL DEFAULT 0 COMMENT '是否在线连接',
  `disconnect_time` datetime NULL DEFAULT NULL COMMENT '断开连接时间',
  `reconnect_count` int(11) NULL DEFAULT 0 COMMENT '重连次数',
  `duration_used` int(11) NULL DEFAULT 0 COMMENT '已用时长',
  `ip_address` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'ip地址',
  `cheat_suspect` tinyint(4) NULL DEFAULT 0 COMMENT '疑似作弊',
  `cheat_evidence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '作弊证据',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_user_attempt`(`exam_id`, `user_id`, `attempt`) USING BTREE,
  UNIQUE INDEX `uk_access_token`(`access_token`) USING BTREE,
  INDEX `idx_exam_status`(`exam_id`, `status`) USING BTREE,
  INDEX `idx_user_exam`(`user_id`, `exam_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_submit_time`(`submit_time`) USING BTREE,
  CONSTRAINT `fk_exam_participants_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_participants_user` FOREIGN KEY (`user_id`) REFERENCES `system_users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试参与表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_participants
-- ----------------------------

-- ----------------------------
-- Table structure for exam_questions
-- ----------------------------
DROP TABLE IF EXISTS `exam_questions`;
CREATE TABLE `exam_questions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `exam_id` bigint(20) NOT NULL COMMENT '考试id',
  `question_id` bigint(20) NOT NULL COMMENT '题目id',
  `question_score` int(11) NOT NULL COMMENT '本题分值',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '题目顺序',
  `group_id` tinyint(4) NULL DEFAULT 1 COMMENT '题目分组id（1：选择 2：填空 3：判断 4：...）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_question`(`exam_id`, `question_id`) USING BTREE,
  INDEX `idx_exam_id`(`exam_id`) USING BTREE,
  INDEX `idx_question_id`(`question_id`) USING BTREE,
  INDEX `idx_sort`(`exam_id`, `sort_order`) USING BTREE,
  INDEX `idx_group`(`exam_id`, `group_id`) USING BTREE,
  CONSTRAINT `fk_exam_questions_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_questions_question` FOREIGN KEY (`question_id`) REFERENCES `questions_bank` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '试卷题目关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_questions
-- ----------------------------

-- ----------------------------
-- Table structure for exams
-- ----------------------------
DROP TABLE IF EXISTS `exams`;
CREATE TABLE `exams`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '考试id',
  `exam_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试名称',
  `exam_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '考试描述',
  `exam_type` tinyint(4) NOT NULL COMMENT '考试类型（1：正式 2：模拟 3：自测 4：竞赛）',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '考试状态（1：未开始 2：进行中 3：已结束 4：已归档）',
  `start_time` datetime NOT NULL COMMENT '考试开始时间',
  `end_time` datetime NOT NULL COMMENT '考试结束时间',
  `duration` int(11) NOT NULL COMMENT '考试时长',
  `total_score` int(11) NOT NULL COMMENT '总分',
  `question_count` int(11) NOT NULL COMMENT '试题总量',
  `creator_id` bigint(20) NOT NULL COMMENT '创建者id',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status_time`(`status`, `start_time`, `end_time`) USING BTREE,
  INDEX `idx_creator`(`creator_id`) USING BTREE,
  INDEX `idx_exam_type`(`exam_type`) USING BTREE,
  CONSTRAINT `fk_exam_creator` FOREIGN KEY (`creator_id`) REFERENCES `system_users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exams
-- ----------------------------

-- ----------------------------
-- Table structure for questions_bank
-- ----------------------------
DROP TABLE IF EXISTS `questions_bank`;
CREATE TABLE `questions_bank`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '题目id',
  `question_category` tinyint(4) NOT NULL COMMENT '题目类型（1：单选 2：多选 3：判断 4：填空 5：简答 6：编程）',
  `question_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '题目内容',
  `question_options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '题目选项（如果是单选/多选）',
  `question_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '题目答案',
  `question_tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '题目标签',
  `is_deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`question_category`) USING BTREE,
  INDEX `idx_tags`(`question_tags`(100)) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions_bank
-- ----------------------------

-- ----------------------------
-- Table structure for questions_statistics
-- ----------------------------
DROP TABLE IF EXISTS `questions_statistics`;
CREATE TABLE `questions_statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键id',
  `question_id` bigint(20) NOT NULL COMMENT '题目id',
  `question_total_user` int(11) NULL DEFAULT 0 COMMENT '总使用次数',
  `question_correct_times` int(11) NULL DEFAULT 0 COMMENT '答对次数',
  `question_avg_duration` int(11) NULL DEFAULT 0 COMMENT '平均答题时长',
  `last_used_time` datetime NULL DEFAULT NULL COMMENT '最后使用时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_question_id`(`question_id`) USING BTREE,
  INDEX `idx_last_used_time`(`last_used_time`) USING BTREE,
  CONSTRAINT `fk_question_stats` FOREIGN KEY (`question_id`) REFERENCES `questions_bank` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题目统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions_statistics
-- ----------------------------

-- ----------------------------
-- Table structure for questions_tags
-- ----------------------------
DROP TABLE IF EXISTS `questions_tags`;
CREATE TABLE `questions_tags`  (
  `tag_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '标签id',
  `tag_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标签名字',
  `is_deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tag_id`) USING BTREE,
  UNIQUE INDEX `uk_tag_name`(`tag_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题目标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions_tags
-- ----------------------------

-- ----------------------------
-- Table structure for system_users
-- ----------------------------
DROP TABLE IF EXISTS `system_users`;
CREATE TABLE `system_users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名字',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户密码（hash加密）',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户登录ip',
  `register_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `user_role` tinyint(4) NULL DEFAULT 1 COMMENT '用户角色（1：学生 2：教师 3：管理员）',
  `is_deleted` tinyint(4) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_role`(`user_role`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_users
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
