/*
 Navicat Premium Data Transfer

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3306
 Source Schema         : orange_tools

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 07/12/2025 23:40:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_feedback
-- ----------------------------
DROP TABLE IF EXISTS `sys_feedback`;
CREATE TABLE `sys_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'suggestion=建议, bug=缺陷',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细内容',
  `contact` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系方式',
  `is_public_check` tinyint(1) NULL DEFAULT 0 COMMENT '用户是否申请公开 (0=否, 1=是)',
  `status` int NULL DEFAULT 0 COMMENT '展示状态: 0=待审核/私密, 1=公开展示, 2=开发中, 3=已上线, 9=已驳回',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `dislike_count` int NULL DEFAULT 0 COMMENT '踩数',
  `reply_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '管理员回复',
  `reply_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '需求反馈表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_feedback
-- ----------------------------
INSERT INTO `sys_feedback` VALUES (1, 'suggestion', 'Test', '测试建议。', '632578328@qq.com', 1, 1, 9, 4, '感谢建议，我们已经列入开发计划！', NULL, '2025-12-07 14:41:30');
INSERT INTO `sys_feedback` VALUES (2, 'suggestion', 'Test2', '测试2', '', 1, 1, 1, 0, NULL, NULL, '2025-12-07 14:43:16');
INSERT INTO `sys_feedback` VALUES (3, 'suggestion', 'Test3', '测试3.', '', 1, 9, 0, 0, NULL, NULL, '2025-12-07 15:13:25');
INSERT INTO `sys_feedback` VALUES (4, 'suggestion', '1', '1', '', 1, 1, 0, 0, NULL, NULL, '2025-12-07 15:33:15');
INSERT INTO `sys_feedback` VALUES (5, 'suggestion', '2', '2', '', 1, 1, 0, 0, NULL, NULL, '2025-12-07 15:33:20');
INSERT INTO `sys_feedback` VALUES (6, 'suggestion', '3', '3', '', 0, 0, 0, 0, NULL, NULL, '2025-12-07 15:34:21');
INSERT INTO `sys_feedback` VALUES (7, 'suggestion', '4', '4', '', 1, 1, 0, 0, NULL, NULL, '2025-12-07 15:39:21');
INSERT INTO `sys_feedback` VALUES (8, 'suggestion', '5', '5', '', 1, 1, 0, 0, NULL, NULL, '2025-12-07 15:39:30');
INSERT INTO `sys_feedback` VALUES (9, 'suggestion', '6', '6', '', 1, 1, 0, 0, NULL, NULL, '2025-12-07 15:39:33');

-- ----------------------------
-- Table structure for sys_tool
-- ----------------------------
DROP TABLE IF EXISTS `sys_tool`;
CREATE TABLE `sys_tool`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工具名称',
  `usage_count` bigint NULL DEFAULT 0 COMMENT '使用次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工具统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_tool
-- ----------------------------
INSERT INTO `sys_tool` VALUES (1, 'course_tool', '课表空闲统计助手', 1029, '2025-12-07 13:58:44', '2025-12-07 23:20:49');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号 (可以是学号，也可以是自定义的用户名)',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码 (BCrypt加密)',
  `student_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学号 (仅本校学生绑定，用于教务对接)',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `qq_open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'QQ OpenID',
  `wx_open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信 OpenID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'USER' COMMENT '角色: USER(普通用户), ADMIN(管理员)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态: 1-正常, 0-封禁',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_student_id`(`student_id` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户核心表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'orange001', '$2a$10$PMAuDkH/V94P4yNobeuTxOhyJARyhjY/c3aKHE.lBL9LfwMSnwgHq', '2350203170', '玉衡', 'avatar-1.png', NULL, NULL, NULL, NULL, 'ADMIN', 1, '2025-12-05 22:32:18', '2025-12-07 22:16:24');
INSERT INTO `sys_user` VALUES (2, 'orange002', '$2a$10$v9Yfmo8ldG0RxJQZN9AQ/.dc6N7f2CL6U/9Bx78p4MiwQfvnqdL0O', '', 'orange002', NULL, NULL, NULL, NULL, NULL, 'USER', 1, '2025-12-06 17:54:45', '2025-12-06 17:54:45');

SET FOREIGN_KEY_CHECKS = 1;
