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

 Date: 11/12/2025 22:11:22
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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试参与表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_participants
-- ----------------------------
INSERT INTO `exam_participants` VALUES (1, 1, 1, 1, '2025-12-11 21:04:26', '2025-12-11 21:04:26', '2025-12-11 21:06:50', 2, '995c1fc59b734deab9b32a0c1497c176', NULL, 1, NULL, 0, 0, NULL, 0, NULL, '2025-12-11 21:04:26', '2025-12-11 21:06:50');
INSERT INTO `exam_participants` VALUES (2, 1, 4, 1, '2025-12-11 21:39:07', '2025-12-11 21:39:07', NULL, 1, '87a764ea1b65486799867ca1d9a02121', NULL, 1, NULL, 0, 0, NULL, 0, NULL, '2025-12-11 21:39:07', '2025-12-11 21:39:07');

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exams
-- ----------------------------
INSERT INTO `exams` VALUES (1, 'test2', 'test2', 1, 2, '2025-12-11 20:00:00', '2025-12-11 23:00:00', 100, 0, 0, 2, '2025-12-11 19:33:27', '2025-12-11 19:33:27', 0);

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
) ENGINE = InnoDB AUTO_INCREMENT = 101 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions_bank
-- ----------------------------
INSERT INTO `questions_bank` VALUES (1, 1, 'Java语言中，int类型占用多少字节？', '[\"1\", \"2\", \"4\", \"8\"]', 'C', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (2, 1, '下面哪个不是Java的基本数据类型？', '[\"int\", \"String\", \"boolean\", \"double\"]', 'B', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (3, 1, 'Java中，用于继承的关键字是？', '[\"extends\", \"implements\", \"inherit\", \"super\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (4, 1, '以下哪个修饰符表示包访问权限？', '[\"public\", \"private\", \"protected\", \"默认（无修饰符）\"]', 'D', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (5, 1, 'Java中，Math.round(11.5)的结果是？', '[\"11\", \"11.5\", \"12\", \"12.0\"]', 'C', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (6, 1, '下面哪个不是Java的关键字？', '[\"native\", \"sizeof\", \"strictfp\", \"transient\"]', 'B', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (7, 1, 'Java中，接口中的方法默认是什么访问修饰符？', '[\"public\", \"private\", \"protected\", \"default\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (8, 1, '哪个集合类是线程安全的？', '[\"ArrayList\", \"HashMap\", \"Vector\", \"LinkedList\"]', 'C', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (9, 1, 'Java中，String类的equals()方法比较的是？', '[\"引用地址\", \"字符串内容\", \"字符串长度\", \"字符串编码\"]', 'B', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (10, 1, 'try-catch-finally语句中，finally块一定会执行吗？', '[\"总是执行\", \"发生异常时不执行\", \"catch块有return时不执行\", \"System.exit(0)时不执行\"]', 'D', 'Java基础,异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (11, 1, 'Java中，==运算符用于比较对象时，比较的是？', '[\"对象内容\", \"对象引用\", \"对象类型\", \"对象hashCode\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (12, 1, '哪个关键字用于创建类的实例？', '[\"new\", \"this\", \"super\", \"instance\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (13, 1, 'Java中，final修饰的变量表示什么？', '[\"可变\", \"不可变\", \"静态\", \"抽象\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (14, 1, 'ArrayList的默认初始容量是多少？', '[\"5\", \"10\", \"15\", \"20\"]', 'B', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (15, 1, 'Java中，哪个包包含Scanner类？', '[\"java.lang\", \"java.util\", \"java.io\", \"java.text\"]', 'B', 'Java基础,IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (16, 1, '哪个关键字用于从方法中返回值？', '[\"return\", \"void\", \"break\", \"continue\"]', 'A', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (17, 1, 'Java中，main方法的正确声明是？', '[\"public static int main(String[] args)\", \"public static void main(String[] args)\", \"static void main(String[] args)\", \"public void main(String[] args)\"]', 'B', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (18, 1, '哪个集合类允许重复元素？', '[\"Set\", \"Map\", \"List\", \"Queue\"]', 'C', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (19, 1, 'Java中，byte类型的取值范围是？', '[\"-128 ~ 127\", \"0 ~ 255\", \"-32768 ~ 32767\", \"0 ~ 65535\"]', 'A', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (20, 1, '哪个运算符用于判断对象是否为某个类的实例？', '[\"instanceof\", \"typeof\", \"isInstance\", \"getClass\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (21, 1, 'Java中，垃圾回收的主要作用是什么？', '[\"释放内存\", \"提高性能\", \"防止内存泄漏\", \"优化代码\"]', 'A', 'Java基础,JVM', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (22, 1, '哪个异常是RuntimeException的子类？', '[\"IOException\", \"SQLException\", \"NullPointerException\", \"ClassNotFoundException\"]', 'C', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (23, 1, 'Java中，StringBuffer和StringBuilder的主要区别是什么？', '[\"功能不同\", \"线程安全性\", \"性能\", \"存储方式\"]', 'B', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (24, 1, '哪个关键字用于声明抽象类？', '[\"abstract\", \"interface\", \"final\", \"static\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (25, 1, 'Java中，哪个方法用于获取数组的长度？', '[\"size()\", \"length()\", \"length\", \"getSize()\"]', 'C', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (26, 1, '哪个集合接口不允许重复元素？', '[\"List\", \"Set\", \"Map\", \"Queue\"]', 'B', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (27, 1, 'Java中，super关键字的作用是？', '[\"调用父类方法\", \"调用子类方法\", \"创建对象\", \"声明变量\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (28, 1, '哪个关键字用于处理异常？', '[\"try\", \"catch\", \"finally\", \"以上都是\"]', 'D', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (29, 1, 'Java中，哪种循环至少执行一次？', '[\"for\", \"while\", \"do-while\", \"foreach\"]', 'C', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (30, 1, '哪个修饰符表示静态方法？', '[\"static\", \"final\", \"abstract\", \"synchronized\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (31, 1, 'HashMap允许null键吗？', '[\"允许\", \"不允许\", \"有时允许\", \"取决于容量\"]', 'A', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (32, 1, 'Java中，线程的生命周期不包括哪个状态？', '[\"新建\", \"就绪\", \"运行\", \"销毁\"]', 'D', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (33, 1, '哪个包包含Java的核心类？', '[\"java.util\", \"java.io\", \"java.lang\", \"java.net\"]', 'C', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (34, 1, 'Java中，方法的参数传递是什么方式？', '[\"值传递\", \"引用传递\", \"地址传递\", \"指针传递\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (35, 1, '哪个关键字用于同步代码块？', '[\"synchronized\", \"volatile\", \"transient\", \"native\"]', 'A', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (36, 1, 'Java中，Object类是所有类的什么？', '[\"子类\", \"父类\", \"基类\", \"超类\"]', 'D', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (37, 1, '哪个集合类是有序且允许重复的？', '[\"HashSet\", \"TreeSet\", \"ArrayList\", \"HashMap\"]', 'C', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (38, 1, 'Java中，哪个修饰符表示常量？', '[\"static final\", \"final static\", \"const\", \"final\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (39, 1, '哪个运算符用于三目运算？', '[\"? :\", \"&&\", \"||\", \"!\"]', 'A', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (40, 1, 'Java中，接口可以包含什么？', '[\"抽象方法\", \"默认方法\", \"静态方法\", \"以上都是\"]', 'D', '面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (41, 2, '以下哪些是Java的基本数据类型？', '[\"int\", \"String\", \"boolean\", \"float\", \"Object\"]', 'A,C,D', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (42, 2, 'Java中，哪些修饰符可以修饰类？', '[\"public\", \"private\", \"protected\", \"abstract\", \"final\"]', 'A,D,E', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (43, 2, '以下哪些是运行时异常？', '[\"NullPointerException\", \"IOException\", \"ArrayIndexOutOfBoundsException\", \"ClassCastException\", \"SQLException\"]', 'A,C,D', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (44, 2, 'Java中，哪些集合类是线程安全的？', '[\"ArrayList\", \"Vector\", \"HashMap\", \"Hashtable\", \"ConcurrentHashMap\"]', 'B,D,E', '集合框架,多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (45, 2, '以下哪些是Java的关键字？', '[\"goto\", \"const\", \"sizeof\", \"native\", \"strictfp\"]', 'A,B,D,E', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (46, 2, 'Java中，哪些方法可以重写？', '[\"private方法\", \"public方法\", \"protected方法\", \"final方法\", \"static方法\"]', 'B,C', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (47, 2, '以下哪些是Java的访问修饰符？', '[\"public\", \"private\", \"protected\", \"default\", \"internal\"]', 'A,B,C,D', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (48, 2, 'Java中，哪些是合法的标识符？', '[\"_name\", \"$value\", \"2var\", \"class\", \"myVar\"]', 'A,B,E', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (49, 2, '以下哪些是Object类的方法？', '[\"equals()\", \"hashCode()\", \"toString()\", \"wait()\", \"notify()\"]', 'A,B,C,D,E', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (50, 2, 'Java中，哪些是创建线程的方式？', '[\"继承Thread类\", \"实现Runnable接口\", \"实现Callable接口\", \"使用线程池\", \"使用Timer\"]', 'A,B,C', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (51, 2, '以下哪些是集合框架的接口？', '[\"List\", \"Set\", \"Map\", \"Collection\", \"Arrays\"]', 'A,B,C,D', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (52, 2, 'Java中，哪些语句会导致循环终止？', '[\"break\", \"continue\", \"return\", \"System.exit(0)\", \"throw\"]', 'A,C,D,E', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (53, 2, '以下哪些是Java的包装类？', '[\"Integer\", \"int\", \"Boolean\", \"String\", \"Character\"]', 'A,C,E', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (54, 2, 'Java中，哪些可以用来处理字符串？', '[\"String\", \"StringBuffer\", \"StringBuilder\", \"char[]\", \"StringTokenizer\"]', 'A,B,C,D,E', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (55, 2, '以下哪些是JVM的内存区域？', '[\"堆\", \"栈\", \"方法区\", \"程序计数器\", \"本地方法栈\"]', 'A,B,C,D,E', 'JVM', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (56, 2, 'Java中，哪些可以用来进行文件读写？', '[\"FileInputStream\", \"FileReader\", \"BufferedReader\", \"Scanner\", \"FileWriter\"]', 'A,B,C,D,E', 'IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (57, 2, '以下哪些是设计模式？', '[\"单例模式\", \"工厂模式\", \"观察者模式\", \"策略模式\", \"代理模式\"]', 'A,B,C,D,E', '设计模式', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (58, 2, 'Java中，哪些是合法的类型转换？', '[\"int转double\", \"double转int\", \"String转int\", \"Object转String\", \"char转int\"]', 'A,B,C,D,E', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (59, 2, '以下哪些是Java的网络编程类？', '[\"Socket\", \"ServerSocket\", \"URL\", \"HttpURLConnection\", \"InetAddress\"]', 'A,B,C,D,E', '网络编程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (60, 2, 'Java中，哪些可以用来实现同步？', '[\"synchronized关键字\", \"volatile关键字\", \"Lock接口\", \"Atomic类\", \"ThreadLocal\"]', 'A,B,C,D,E', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (61, 3, 'Java中，一个类可以实现多个接口。', '[\"正确\", \"错误\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (62, 3, 'Java中，抽象类可以被实例化。', '[\"正确\", \"错误\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (63, 3, 'String类是不可变类。', '[\"正确\", \"错误\"]', 'A', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (64, 3, 'Java中，基本数据类型是值传递，对象是引用传递。', '[\"正确\", \"错误\"]', 'A', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (65, 3, 'final修饰的类可以被继承。', '[\"正确\", \"错误\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (66, 3, 'Java中，try块可以没有catch块，但必须有finally块。', '[\"正确\", \"错误\"]', 'B', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (67, 3, 'ArrayList和LinkedList都是线程安全的。', '[\"正确\", \"错误\"]', 'B', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (68, 3, 'Java中，接口可以包含具体方法的实现。', '[\"正确\", \"错误\"]', 'A', '面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (69, 3, 'Java中，==运算符比较对象时比较的是内容。', '[\"正确\", \"错误\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (70, 3, 'static方法可以访问非静态成员变量。', '[\"正确\", \"错误\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (71, 3, 'Java中，垃圾回收器可以保证程序不会出现内存泄漏。', '[\"正确\", \"错误\"]', 'B', 'JVM', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (72, 3, 'HashMap允许键和值都为null。', '[\"正确\", \"错误\"]', 'A', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (73, 3, 'Java中，子类可以重写父类的private方法。', '[\"正确\", \"错误\"]', 'B', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (74, 3, 'Java中，finally块中的代码总是会执行。', '[\"正确\", \"错误\"]', 'B', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (75, 3, 'Java中，数组也是对象。', '[\"正确\", \"错误\"]', 'A', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (76, 4, 'Java语言是由______公司开发的。', NULL, 'Sun', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (77, 4, 'Java中，用于单行注释的符号是______。', NULL, '//', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (78, 4, 'Java中，所有类的根类是______。', NULL, 'Object', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (79, 4, 'Java中，表示继承的关键字是______。', NULL, 'extends', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (80, 4, 'Java中，表示接口实现的关键字是______。', NULL, 'implements', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (81, 4, 'Java中，用于创建线程的类名是______。', NULL, 'Thread', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (82, 4, 'Java中，用于表示字符串的类是______。', NULL, 'String', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (83, 4, 'Java中，用于处理异常的类名是______。', NULL, 'Exception', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (84, 4, 'Java中，表示静态的关键字是______。', NULL, 'static', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (85, 4, 'Java中，用于从控制台读取输入的类名是______。', NULL, 'Scanner', 'Java基础,IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (86, 4, 'Java中，集合框架的根接口是______。', NULL, 'Collection', '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (87, 4, 'Java中，用于文件读写的抽象类是______。', NULL, 'InputStream/OutputStream', 'IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (88, 4, 'Java中，表示常量的修饰符组合是______。', NULL, 'static final', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (89, 4, 'Java中，用于多态的关键机制是______。', NULL, '方法重写', 'Java基础,面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (90, 4, 'Java中，用于同步代码块的关键字是______。', NULL, 'synchronized', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (91, 5, '请简述Java中抽象类和接口的区别。', NULL, '1. 抽象类可以有构造方法，接口不能有；2. 抽象类可以有普通成员变量，接口只能有常量；3. 抽象类可以有具体方法，接口在Java 8之前只能有抽象方法；4. 一个类只能继承一个抽象类，但可以实现多个接口；5. 抽象类用于表示\"is-a\"关系，接口表示\"has-a\"能力。', '面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (92, 5, '请解释Java中的多态性。', NULL, '多态是面向对象的三大特性之一，指同一操作作用于不同的对象，可以有不同的解释，产生不同的执行结果。Java中多态主要通过方法重写和方法重载实现。运行时多态通过方法重写实现，编译时多态通过方法重载实现。', '面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (93, 5, '请简述Java垃圾回收机制的原理。', NULL, 'Java垃圾回收机制通过自动管理内存，回收不再使用的对象所占用的内存空间。主要使用引用计数法和可达性分析法判断对象是否可回收。垃圾回收器会定期运行，清理不可达对象，并进行内存整理。', 'JVM', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (94, 5, '请解释Java中synchronized关键字的作用。', NULL, 'synchronized关键字用于实现线程同步，保证同一时刻只有一个线程可以访问被synchronized修饰的代码块或方法。它可以修饰实例方法、静态方法和代码块，确保线程安全，防止数据不一致。', '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (95, 5, '请简述Java异常处理机制。', NULL, 'Java异常处理通过try-catch-finally语句实现。try块包含可能抛出异常的代码，catch块捕获并处理异常，finally块无论是否发生异常都会执行。异常分为检查异常和运行时异常，都继承自Throwable类。', '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (96, 6, '编写一个Java程序，实现一个单例模式。', NULL, 'public class Singleton {\r\n    private static Singleton instance;\r\n    \r\n    private Singleton() {}\r\n    \r\n    public static Singleton getInstance() {\r\n        if (instance == null) {\r\n            synchronized (Singleton.class) {\r\n                if (instance == null) {\r\n                    instance = new Singleton();\r\n                }\r\n            }\r\n        }\r\n        return instance;\r\n    }\r\n}', '设计模式', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (97, 6, '编写一个Java程序，实现冒泡排序算法。', NULL, 'public class BubbleSort {\r\n    public static void bubbleSort(int[] arr) {\r\n        int n = arr.length;\r\n        for (int i = 0; i < n - 1; i++) {\r\n            for (int j = 0; j < n - i - 1; j++) {\r\n                if (arr[j] > arr[j + 1]) {\r\n                    int temp = arr[j];\r\n                    arr[j] = arr[j + 1];\r\n                    arr[j + 1] = temp;\r\n                }\r\n            }\r\n        }\r\n    }\r\n}', 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (98, 6, '编写一个Java程序，统计字符串中每个字符出现的次数。', NULL, 'import java.util.HashMap;\r\nimport java.util.Map;\r\n\r\npublic class CharCounter {\r\n    public static Map<Character, Integer> countChars(String str) {\r\n        Map<Character, Integer> map = new HashMap<>();\r\n        for (char c : str.toCharArray()) {\r\n            map.put(c, map.getOrDefault(c, 0) + 1);\r\n        }\r\n        return map;\r\n    }\r\n}', 'Java基础,集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (99, 6, '编写一个Java程序，实现生产者消费者模式。', NULL, 'import java.util.LinkedList;\r\nimport java.util.Queue;\r\n\r\npublic class ProducerConsumer {\r\n    private Queue<Integer> queue = new LinkedList<>();\r\n    private int capacity = 5;\r\n    \r\n    public void produce() throws InterruptedException {\r\n        int value = 0;\r\n        while (true) {\r\n            synchronized (this) {\r\n                while (queue.size() == capacity) {\r\n                    wait();\r\n                }\r\n                queue.add(value++);\r\n                System.out.println(\"Produced: \" + value);\r\n                notify();\r\n                Thread.sleep(1000);\r\n            }\r\n        }\r\n    }\r\n    \r\n    public void consume() throws InterruptedException {\r\n        while (true) {\r\n            synchronized (this) {\r\n                while (queue.isEmpty()) {\r\n                    wait();\r\n                }\r\n                int value = queue.poll();\r\n                System.out.println(\"Consumed: \" + value);\r\n                notify();\r\n                Thread.sleep(1000);\r\n            }\r\n        }\r\n    }\r\n}', '多线程,设计模式', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_bank` VALUES (100, 6, '编写一个Java程序，实现文件的复制功能。', NULL, 'import java.io.FileInputStream;\r\nimport java.io.FileOutputStream;\r\nimport java.io.IOException;\r\n\r\npublic class FileCopy {\r\n    public static void copyFile(String source, String destination) throws IOException {\r\n        try (FileInputStream fis = new FileInputStream(source);\r\n             FileOutputStream fos = new FileOutputStream(destination)) {\r\n            byte[] buffer = new byte[1024];\r\n            int length;\r\n            while ((length = fis.read(buffer)) > 0) {\r\n                fos.write(buffer, 0, length);\r\n            }\r\n        }\r\n    }\r\n}', 'IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');

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
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题目标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions_tags
-- ----------------------------
INSERT INTO `questions_tags` VALUES (1, 'Java基础', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (2, '面向对象', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (3, '集合框架', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (4, '异常处理', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (5, '多线程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (6, 'IO流', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (7, 'JDBC', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (8, '网络编程', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (9, 'JVM', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');
INSERT INTO `questions_tags` VALUES (10, '设计模式', 0, '2025-12-11 21:02:56', '2025-12-11 21:02:56');

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
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_users
-- ----------------------------
INSERT INTO `system_users` VALUES (1, 'student1', '$2a$10$fSXD9soV3Ovo0bKlg8qwz.efFlHzQQk1JXBqkYLIdOEeMvCnjPCqi', 1, '2025-12-11 19:29:29', NULL, '2025-12-11 19:29:29', '2025-12-11 19:29:29', 1, 0);
INSERT INTO `system_users` VALUES (2, 'admin1', '$2a$10$ntDHlRx3STqm7xGXv.Bb2.AkOopM/uOm5DHf8Xh8cl/lmGwrkgd6S', 1, '2025-12-11 19:29:46', NULL, '2025-12-11 19:29:46', '2025-12-11 19:29:46', 3, 0);
INSERT INTO `system_users` VALUES (3, 'admin2', '$2a$10$FVXOm0w0sS.ch6dr.vizLOlF/I00wZ2H7rjBGxOXW0TscRgolBTP6', 1, '2025-12-11 21:20:13', NULL, '2025-12-11 21:20:13', '2025-12-11 21:20:13', 3, 0);
INSERT INTO `system_users` VALUES (4, 'student2', '$2a$10$/ZROIUF4b8uYVaqf4Fm2fuoKfl9PrVDNmLTPgoJWS0qsl6l0RpJOy', 1, '2025-12-11 21:32:47', NULL, '2025-12-11 21:29:34', '2025-12-11 21:29:34', 1, 0);
INSERT INTO `system_users` VALUES (5, 'student3', '$2a$10$vn9SDwQulMQCjtSLeo60L.DNCmPwtbSOTq32HwergAddTWIlUcs4O', 1, '2025-12-11 21:32:05', NULL, '2025-12-11 21:32:05', '2025-12-11 21:32:05', 1, 0);

SET FOREIGN_KEY_CHECKS = 1;
