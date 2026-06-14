-- ============================================
-- 智能电子商城 - 数据库一键初始化脚本
-- 版本：1.0.0
-- 创建时间：2026-06-14
-- 说明：执行此脚本即可完成数据库的完整初始化
-- 使用方法：mysql -u root -p < init-database.sql
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS `db_mall` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `db_mall`;

-- ============================================
-- 2. 创建基础表结构
-- ============================================

-- 2.1 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `role` varchar(20) DEFAULT 'customer' COMMENT '用户角色：customer(普通用户)/service(客服)/admin(管理员)',
  `nickname` varchar(255) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_delete` tinyint(1) DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2.2 地址表
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `link_user` varchar(255) DEFAULT NULL COMMENT '联系人',
  `link_address` varchar(255) DEFAULT NULL COMMENT '地址',
  `link_phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `user_id` bigint(20) DEFAULT NULL COMMENT '所属用户',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址表';

-- 2.3 头像表
DROP TABLE IF EXISTS `avatar`;
CREATE TABLE `avatar` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` varchar(255) DEFAULT NULL,
  `size` bigint(20) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `md5` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='头像表';

-- 2.4 商品分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) DEFAULT NULL COMMENT '分类名称',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父分类ID',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `icon` varchar(255) DEFAULT NULL COMMENT '图标',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_delete` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2.5 商品表
DROP TABLE IF EXISTS `good`;
CREATE TABLE `good` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) DEFAULT NULL COMMENT '商品名称',
  `description` text COMMENT '商品描述',
  `discount` decimal(10,2) DEFAULT 1.00 COMMENT '折扣',
  `sales` int(11) DEFAULT 0 COMMENT '销量',
  `review_count` int(11) DEFAULT 0 COMMENT '评论总数',
  `good_rating` decimal(3,2) DEFAULT 5.00 COMMENT '商品评分（1-5分）',
  `rating_5_count` int(11) DEFAULT 0 COMMENT '5星评价数量',
  `rating_4_count` int(11) DEFAULT 0 COMMENT '4星评价数量',
  `rating_3_count` int(11) DEFAULT 0 COMMENT '3星评价数量',
  `rating_2_count` int(11) DEFAULT 0 COMMENT '2星评价数量',
  `rating_1_count` int(11) DEFAULT 0 COMMENT '1星评价数量',
  `tags` varchar(1000) DEFAULT NULL COMMENT '商品标签',
  `sale_money` decimal(10,2) DEFAULT 0.00 COMMENT '销售金额',
  `category_id` bigint(20) DEFAULT NULL COMMENT '分类ID',
  `imgs` varchar(255) DEFAULT NULL COMMENT '商品图片',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `recommend` tinyint(1) DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
  `is_delete` tinyint(1) DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_sales` (`sales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2.6 商品规格表
DROP TABLE IF EXISTS `good_standard`;
CREATE TABLE `good_standard` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `good_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `value` varchar(255) DEFAULT NULL COMMENT '规格值',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `store` int(11) DEFAULT 0 COMMENT '库存',
  PRIMARY KEY (`id`),
  KEY `idx_good_id` (`good_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格表';

-- 2.7 购物车表
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `count` int(11) DEFAULT NULL COMMENT '数量',
  `create_time` datetime DEFAULT NULL COMMENT '加入时间',
  `good_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `standard` varchar(255) DEFAULT NULL COMMENT '规格',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 2.8 订单表
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(255) DEFAULT NULL COMMENT '订单号',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `total_money` decimal(10,2) DEFAULT NULL COMMENT '订单总金额',
  `pay_money` decimal(10,2) DEFAULT NULL COMMENT '支付金额',
  `pay_type` varchar(255) DEFAULT NULL COMMENT '支付方式',
  `status` int(11) DEFAULT 0 COMMENT '订单状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
  `address` varchar(255) DEFAULT NULL COMMENT '收货地址',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 2.9 订单商品表
DROP TABLE IF EXISTS `order_good`;
CREATE TABLE `order_good` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单ID',
  `good_id` bigint(20) DEFAULT NULL COMMENT '商品ID',
  `count` int(11) DEFAULT NULL COMMENT '数量',
  `standard` varchar(255) DEFAULT NULL COMMENT '规格',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表';

-- 2.10 轮播图表
DROP TABLE IF EXISTS `carousel`;
CREATE TABLE `carousel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `good_id` bigint(20) DEFAULT NULL COMMENT '对应的商品ID',
  `show_order` int(11) DEFAULT NULL COMMENT '播放顺序',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='轮播图表';

-- 2.11 分类图标表
DROP TABLE IF EXISTS `category_icon`;
CREATE TABLE `category_icon` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `icon_url` varchar(500) NOT NULL COMMENT '图标 URL',
  `icon_emoji` varchar(50) DEFAULT NULL COMMENT '图标 Emoji',
  `bg_color` varchar(20) DEFAULT '#e8f4f8' COMMENT '背景颜色',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类图标表';

-- ============================================
-- 3. AI 相关表
-- ============================================

-- 3.1 AI 对话历史表
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `title` varchar(200) DEFAULT '新对话' COMMENT '对话标题',
  `type` varchar(50) DEFAULT 'chat' COMMENT '对话类型：chat(聊天)/search(搜索)/recommend(推荐)/order(订单)',
  `messages` json DEFAULT NULL COMMENT '对话消息列表（JSON格式）',
  `last_message_time` datetime DEFAULT NULL COMMENT '最后一条消息时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史表';

-- 3.2 聊天消息表
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ticket_id` bigint(20) DEFAULT NULL COMMENT '工单ID',
  `sender_id` bigint(20) NOT NULL COMMENT '发送者ID',
  `sender_role` varchar(20) NOT NULL COMMENT '发送者角色：user/service',
  `receiver_id` bigint(20) DEFAULT NULL COMMENT '接收者ID',
  `content` text COMMENT '消息内容',
  `message_type` varchar(20) DEFAULT 'text' COMMENT '消息类型：text/image/file/system',
  `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读：0-未读/1-已读',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `read_at` datetime DEFAULT NULL COMMENT '读取时间',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ============================================
-- 4. 工单系统表
-- ============================================

-- 4.1 工单表
DROP TABLE IF EXISTS `ticket`;
CREATE TABLE `ticket` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ticket_no` varchar(32) UNIQUE NOT NULL COMMENT '工单号（格式：TKT-YYYYMMDD-XXXX）',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `category` varchar(50) NOT NULL COMMENT '工单分类：technical(技术)/billing(账单)/product(商品)/complaint(投诉)/other(其他)',
  `subject` varchar(200) NOT NULL COMMENT '工单主题',
  `description` text NOT NULL COMMENT '问题详细描述',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending(待处理)/processing(处理中)/resolved(已解决)/closed(已关闭)',
  `priority` varchar(20) DEFAULT 'normal' COMMENT '优先级：low(低)/normal(普通)/high(高)/urgent(紧急)',
  `assigned_to` bigint(20) DEFAULT NULL COMMENT '分配给的客服ID（NULL表示未分配）',
  `created_by` bigint(20) DEFAULT NULL COMMENT '创建人ID（通常是用户自己）',
  `queue_position` int(11) DEFAULT NULL COMMENT '排队位置',
  `chat_room_id` varchar(64) DEFAULT NULL COMMENT '聊天室ID',
  `chat_started_at` datetime DEFAULT NULL COMMENT '聊天开始时间',
  `chat_ended_at` datetime DEFAULT NULL COMMENT '聊天结束时间',
  `ended_by` varchar(20) DEFAULT NULL COMMENT '结束方：user/service',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `resolved_at` datetime DEFAULT NULL COMMENT '解决时间',
  `closed_at` datetime DEFAULT NULL COMMENT '关闭时间',
  `satisfaction_score` tinyint(4) DEFAULT NULL COMMENT '满意度评分：1-5星',
  `satisfaction_comment` varchar(500) DEFAULT NULL COMMENT '满意度评价内容',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_assigned_to` (`assigned_to`),
  KEY `idx_ticket_no` (`ticket_no`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单表';

-- 4.2 工单流转记录表
DROP TABLE IF EXISTS `ticket_history`;
CREATE TABLE `ticket_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ticket_id` bigint(20) NOT NULL COMMENT '工单ID',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作人ID（用户或客服）',
  `operator_role` varchar(20) DEFAULT 'customer' COMMENT '操作人角色：customer(用户)/service(客服)/system(系统)',
  `action` varchar(50) NOT NULL COMMENT '操作类型：create(创建)/assign(分配)/transfer(转交)/reply(回复)/resolve(解决)/close(关闭)/reopen(重新打开)',
  `remark` varchar(500) DEFAULT NULL COMMENT '操作备注',
  `old_value` varchar(200) DEFAULT NULL COMMENT '旧值（如原状态）',
  `new_value` varchar(200) DEFAULT NULL COMMENT '新值（如新状态）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转记录表';

-- 4.3 工单回复表
DROP TABLE IF EXISTS `ticket_reply`;
CREATE TABLE `ticket_reply` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ticket_id` bigint(20) NOT NULL COMMENT '工单ID',
  `reply_from` bigint(20) NOT NULL COMMENT '回复人ID',
  `reply_from_role` varchar(20) DEFAULT 'customer' COMMENT '回复人角色：customer/service',
  `content` text NOT NULL COMMENT '回复内容',
  `is_internal` tinyint(1) DEFAULT 0 COMMENT '是否内部备注（1=仅客服可见，0=用户可见）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
  PRIMARY KEY (`id`),
  KEY `idx_ticket_id` (`ticket_id`),
  KEY `idx_reply_from` (`reply_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单回复表';

-- ============================================
-- 5. 评论系统表
-- ============================================

-- 5.1 评论表
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `good_id` bigint(20) NOT NULL COMMENT '商品ID',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单ID（可选，验证购买）',
  `rating` int(1) NOT NULL COMMENT '评分：1-5星',
  `content` text COMMENT '评论内容',
  `images` varchar(2000) DEFAULT NULL COMMENT '评论图片（多张图片用逗号分隔）',
  `tags` varchar(500) DEFAULT NULL COMMENT '评论标签（如：质量好、物流快、包装精美）',
  `reply_count` int(11) DEFAULT 0 COMMENT '回复数量',
  `like_count` int(11) DEFAULT 0 COMMENT '点赞数量',
  `dislike_count` int(11) DEFAULT 0 COMMENT '点踩数量',
  `is_anonymous` tinyint(1) DEFAULT 0 COMMENT '是否匿名：0-否，1-是',
  `status` int(1) DEFAULT 1 COMMENT '状态：0-待审核，1-已发布，2-已屏蔽',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_good_id` (`good_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_rating` (`rating`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';

-- 5.2 评论回复表
DROP TABLE IF EXISTS `review_reply`;
CREATE TABLE `review_reply` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '回复ID',
  `review_id` bigint(20) NOT NULL COMMENT '评论ID',
  `user_id` bigint(20) NOT NULL COMMENT '回复用户ID',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父回复ID（用于楼中楼）',
  `to_user_id` bigint(20) DEFAULT NULL COMMENT '被回复用户ID',
  `content` text NOT NULL COMMENT '回复内容',
  `like_count` int(11) DEFAULT 0 COMMENT '点赞数量',
  `status` int(1) DEFAULT 1 COMMENT '状态：0-待审核，1-已发布，2-已屏蔽',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论回复表';

-- 5.3 评论图片表
DROP TABLE IF EXISTS `review_image`;
CREATE TABLE `review_image` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `review_id` bigint(20) NOT NULL COMMENT '评论ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片URL',
  `sort_order` int(11) DEFAULT 0 COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论图片表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 6. 插入测试数据
-- ============================================

-- 6.1 插入测试用户（密码：123456）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`, `email`, `phone`) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin', 'admin@example.com', '13800138000'),
('service001', 'e10adc3949ba59abbe56e057f20f883e', '客服小王', 'service', 'service001@example.com', '13800138001'),
('service002', 'e10adc3949ba59abbe56e057f20f883e', '客服小李', 'service', 'service002@example.com', '13800138002'),
('testuser', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', 'customer', 'test@example.com', '13800138003')
ON DUPLICATE KEY UPDATE `nickname`=VALUES(`nickname`), `role`=VALUES(`role`);

-- 6.2 插入商品分类
INSERT INTO `category` (`id`, `name`, `parent_id`, `sort_order`) VALUES
(1, '服装', NULL, 1),
(2, '鞋类', NULL, 2),
(3, '数码', NULL, 3),
(4, '食品', NULL, 4),
(5, '美妆', NULL, 5),
(6, '家居', NULL, 6),
(7, '运动', NULL, 7),
(8, '母婴', NULL, 8),
(9, '宠物', NULL, 9),
(10, '手机', 3, 1),
(11, '笔记本', 3, 2),
(12, '平板', 3, 3),
(13, '配件', 3, 4),
(14, '男装', 1, 1),
(15, '女装', 1, 2),
(16, '男鞋', 2, 1),
(17, '女鞋', 2, 2)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 6.3 插入分类图标
INSERT INTO `category_icon` (`category_name`, `icon_url`, `icon_emoji`, `bg_color`, `sort_order`) VALUES
('商务笔记本', '/icons/laptop-business.svg', '💼', '#e3f2fd', 1),
('旗舰手机', '/icons/phone-flagship.svg', '📱', '#f3e5f5', 2),
('轻薄笔记本', '/icons/laptop-thin.svg', '💻', '#e8f5e9', 3),
('台式电脑', '/icons/desktop.svg', '🖥️', '#fff3e0', 4),
('游戏手机', '/icons/phone-gaming.svg', '🎮', '#ffebee', 5),
('数码相机', '/icons/camera.svg', '📷', '#f9fbe7', 6),
('拍照手机', '/icons/phone-camera.svg', '📸', '#fce4ec', 7),
('平板电脑', '/icons/tablet.svg', '📟', '#e0f7fa', 8),
('耳机', '/icons/headphone.svg', '🎧', '#f3e5f5', 9),
('音响', '/icons/speaker.svg', '🔊', '#fff8e1', 10),
('智能手表', '/icons/watch.svg', '⌚', '#e8eaf6', 11),
('游戏本', '/icons/laptop-gaming.svg', '🎯', '#ffe0b2', 12),
('办公电脑', '/icons/office.svg', '🖨️', '#c8e6c9', 13),
('学生本', '/icons/student.svg', '📚', '#bbdefb', 14),
('设计师本', '/icons/designer.svg', '🎨', '#f8bbd9', 15),
('默认分类', '/icons/default.svg', '📦', '#f5f5f5', 99)
ON DUPLICATE KEY UPDATE `icon_emoji`=VALUES(`icon_emoji`);

-- 6.4 插入测试商品数据
INSERT INTO `good` (`id`, `name`, `description`, `discount`, `sales`, `sale_money`, `category_id`, `imgs`, `create_time`, `recommend`, `is_delete`) VALUES
-- 服装类
(101, '连帽卫衣男', '2024新款男士连帽卫衣，纯棉面料，舒适透气，宽松版型，多色可选', 0.95, 2580, 15900, 1, '/file/hoodie_black.jpg', '2024-01-15 10:00:00', 1, 0),
(102, '女士圆领卫衣', '韩版宽松圆领卫衣女，加绒加厚，秋冬保暖，简约纯色设计', 0.90, 1890, 12900, 1, '/file/hoodie_pink.jpg', '2024-01-15 10:05:00', 1, 0),
(103, '情侣款卫衣', '情侣装卫衣套装，同款不同色，纯棉面料，舒适亲肤', 0.85, 3200, 18900, 1, '/file/hoodie_couple.jpg', '2024-01-15 10:10:00', 1, 0),
(104, '纯棉T恤男', '夏季男士纯棉T恤，圆领短袖，简约百搭，舒适透气', 0.95, 5680, 7900, 1, '/file/tshirt_white.jpg', '2024-01-15 10:25:00', 1, 0),
(105, '商务衬衫男', '男士商务正装衬衫，免烫面料，修身版型，职场必备', 0.95, 4200, 16900, 1, '/file/shirt_business.jpg', '2024-01-15 10:40:00', 1, 0),
(106, '牛仔裤男', '男士修身牛仔裤，弹力面料，经典蓝色，百搭款', 0.95, 4800, 15900, 1, '/file/jeans_blue.jpg', '2024-01-15 11:15:00', 1, 0),
-- 鞋类
(201, '跑步鞋男', '男士专业跑步鞋，透气网面，减震鞋底，轻便舒适', 0.90, 6800, 25900, 2, '/file/running_black.jpg', '2024-01-15 11:30:00', 1, 0),
(202, '篮球鞋', '专业篮球鞋，高帮设计，防滑耐磨，支撑性好', 0.85, 4200, 39900, 2, '/file/basketball.jpg', '2024-01-15 11:35:00', 1, 0),
(203, '板鞋', '经典板鞋，帆布面料，百搭休闲，学生必备', 0.95, 8900, 12900, 2, '/file/skate_white.jpg', '2024-01-15 11:40:00', 1, 0),
(204, '休闲鞋女', '女士休闲鞋，软底舒适，轻便透气，日常百搭', 0.92, 5600, 15900, 2, '/file/casual_pink.jpg', '2024-01-15 11:45:00', 1, 0),
(205, '帆布鞋', '经典帆布鞋，低帮设计，多色可选，百搭款', 0.95, 7200, 9900, 2, '/file/canvas_black.jpg', '2024-01-15 11:50:00', 1, 0),
-- 数码类 - 手机
(301, '智能手机', '5G全网通智能手机，6.7英寸大屏，128GB存储，多摄像头', 0.95, 12800, 299900, 3, '/file/phone_black.jpg', '2024-01-15 12:25:00', 1, 0),
(302, '游戏手机', '专业游戏手机，骁龙处理器，144Hz高刷屏，散热系统', 0.90, 5600, 399900, 3, '/file/phone_gaming.jpg', '2024-01-15 12:30:00', 1, 0),
(303, '拍照手机', '拍照手机，1亿像素，光学防抖，夜景模式', 0.92, 8900, 349900, 3, '/file/phone_camera.jpg', '2024-01-15 12:35:00', 1, 0),
-- 数码类 - 笔记本
(311, '轻薄笔记本', '轻薄笔记本电脑，14英寸，i5处理器，16GB内存，512GB固态', 0.95, 6800, 499900, 3, '/file/laptop_thin.jpg', '2024-01-15 12:40:00', 1, 0),
(312, '游戏笔记本', '游戏笔记本，15.6英寸，RTX4060，i7处理器，32GB内存', 0.88, 4200, 799900, 3, '/file/laptop_gaming.jpg', '2024-01-15 12:45:00', 1, 0),
(313, '商务笔记本', '商务笔记本电脑，14英寸，i5处理器，轻薄便携，长续航', 0.92, 3500, 399900, 3, '/file/laptop_business.jpg', '2024-01-15 12:50:00', 1, 0),
-- 数码类 - 平板
(321, '平板电脑', '平板电脑，10.9英寸，256GB，WiFi版，学习办公', 0.92, 7800, 299900, 3, '/file/tablet.jpg', '2024-01-15 12:55:00', 1, 0),
-- 数码类 - 配件
(331, '无线蓝牙耳机', '无线蓝牙耳机，主动降噪，长续航，高音质', 0.95, 15800, 29900, 3, '/file/earphone_black.jpg', '2024-01-15 13:05:00', 1, 0),
(332, '智能手表', '智能手表，心率监测，运动追踪，长续航', 0.92, 12800, 199900, 3, '/file/watch_black.jpg', '2024-01-15 13:20:00', 1, 0),
(333, '运动手环', '运动手环，计步器，睡眠监测，防水设计', 0.95, 18900, 9900, 3, '/file/band.jpg', '2024-01-15 13:25:00', 1, 0),
-- 食品类
(401, '坚果礼盒', '混合坚果礼盒，每日坚果，健康零食，送礼佳品', 0.90, 25800, 9900, 4, '/file/nuts.jpg', '2024-01-15 13:30:00', 1, 0),
(402, '薯片', '经典薯片，香脆可口，多种口味，追剧必备', 0.95, 56800, 790, 4, '/file/chips.jpg', '2024-01-15 13:35:00', 1, 0),
(403, '巧克力', '进口巧克力，丝滑口感，浓郁香醇，送礼首选', 0.88, 18900, 2990, 4, '/file/chocolate.jpg', '2024-01-15 13:40:00', 1, 0),
-- 美妆类
(501, '洗面奶', '氨基酸洗面奶，温和清洁，控油保湿，男女通用', 0.95, 35800, 5900, 5, '/file/cleanser.jpg', '2024-01-15 14:10:00', 1, 0),
(502, '爽肤水', '保湿爽肤水，补水滋润，收缩毛孔，清爽不油腻', 0.90, 28900, 8900, 5, '/file/toner.jpg', '2024-01-15 14:15:00', 1, 0),
(503, '精华液', '修护精华液，抗衰老，紧致肌肤，提亮肤色', 0.85, 15600, 29900, 5, '/file/serum.jpg', '2024-01-15 14:20:00', 1, 0),
-- 家居类
(601, '沙发', '现代简约沙发，三人位，科技布面料，舒适透气', 0.85, 5800, 299900, 6, '/file/sofa.jpg', '2024-01-15 14:55:00', 1, 0),
(602, '床', '实木床，1.8米双人床，现代简约，主卧婚床', 0.88, 3500, 199900, 6, '/file/bed.jpg', '2024-01-15 15:00:00', 1, 0),
(603, '四件套', '纯棉四件套，全棉面料，舒适亲肤，简约北欧风', 0.92, 18900, 29900, 6, '/file/bedding.jpg', '2024-01-15 15:15:00', 1, 0),
-- 运动类
(701, '瑜伽垫', '瑜伽垫，加厚10mm，防滑耐磨，健身必备', 0.95, 35800, 5900, 7, '/file/yoga_mat.jpg', '2024-01-15 15:30:00', 1, 0),
(702, '哑铃', '可调节哑铃，男士健身，20kg套装，家用训练', 0.90, 15600, 15900, 7, '/file/dumbbell.jpg', '2024-01-15 15:35:00', 1, 0),
(703, '跑步机', '家用跑步机，折叠设计，静音电机，智能APP控制', 0.85, 2800, 199900, 7, '/file/treadmill.jpg', '2024-01-15 15:40:00', 1, 0),
-- 母婴类
(801, '婴儿奶粉1段', '婴儿配方奶粉1段，0-6个月，进口奶源，营养丰富', 0.95, 25800, 29900, 8, '/file/formula1.jpg', '2024-01-15 16:00:00', 1, 0),
(802, '纸尿裤', '婴儿纸尿裤，超薄透气，防漏设计，S码100片', 0.90, 35800, 15900, 8, '/file/diaper_s.jpg', '2024-01-15 16:10:00', 1, 0),
-- 宠物类
(901, '狗粮', '成犬狗粮，通用型，营养均衡，美毛亮毛', 0.90, 15800, 15900, 9, '/file/dog_food.jpg', '2024-01-15 16:30:00', 1, 0),
(902, '猫粮', '成猫粮，鱼肉味，营养全面，去毛球', 0.92, 18900, 15900, 9, '/file/cat_food.jpg', '2024-01-15 16:35:00', 1, 0);

-- 6.5 插入商品规格
INSERT INTO `good_standard` (`good_id`, `value`, `price`, `store`) VALUES
-- 服装类规格
(101, '黑色 XL', 159.00, 200),
(101, '黑色 XXL', 159.00, 150),
(101, '灰色 L', 159.00, 180),
(102, '粉色 M', 129.00, 200),
(102, '粉色 L', 129.00, 150),
(103, '白色 L', 189.00, 200),
(103, '白色 XL', 189.00, 150),
(104, '白色 L', 79.00, 500),
(104, '白色 XL', 79.00, 400),
(105, '白色 XL', 169.00, 300),
(105, '蓝色 L', 169.00, 250),
(106, '深蓝色 32码', 159.00, 400),
(106, '深蓝色 34码', 159.00, 350),
-- 鞋类规格
(201, '黑色 42码', 259.00, 300),
(201, '黑色 43码', 259.00, 250),
(202, '白色 43码', 399.00, 200),
(202, '黑色 42码', 399.00, 180),
(203, '白色 41码', 129.00, 500),
(203, '白色 42码', 129.00, 450),
(204, '粉色 37码', 159.00, 300),
(204, '粉色 38码', 159.00, 250),
(205, '黑色 40码', 99.00, 600),
(205, '黑色 41码', 99.00, 550),
-- 手机规格
(301, '128GB 黑色', 2999.00, 200),
(301, '256GB 黑色', 3299.00, 150),
(301, '128GB 白色', 2999.00, 180),
(302, '256GB 黑色', 3999.00, 100),
(302, '512GB 黑色', 4599.00, 80),
(303, '128GB 白色', 3499.00, 150),
(303, '256GB 白色', 3799.00, 120),
-- 笔记本规格
(311, '16GB+512GB 银色', 4999.00, 100),
(311, '16GB+1TB 银色', 5499.00, 80),
(312, '32GB+1TB 黑色', 7999.00, 60),
(312, '32GB+2TB 黑色', 8999.00, 40),
(313, '16GB+512GB 深空灰', 3999.00, 120),
(313, '16GB+1TB 深空灰', 4499.00, 100),
-- 平板规格
(321, '256GB WiFi版', 2999.00, 150),
(321, '512GB WiFi版', 3499.00, 100),
-- 配件规格
(331, '黑色', 299.00, 500),
(331, '白色', 299.00, 450),
(332, '黑色', 1999.00, 200),
(332, '银色', 1999.00, 180),
(333, '黑色', 99.00, 800),
-- 食品规格
(401, '750g', 99.00, 500),
(402, '104g', 7.90, 2000),
(403, '100g', 29.90, 800),
-- 美妆规格
(501, '100g', 59.00, 600),
(502, '150ml', 89.00, 500),
(503, '30ml', 299.00, 300),
-- 家居规格
(601, '灰色 三人位', 2999.00, 50),
(602, '原木色 1.8米', 1999.00, 30),
(603, '灰色 1.8m床', 299.00, 200),
-- 运动规格
(701, '紫色 10mm', 59.00, 500),
(702, '20kg 黑色', 159.00, 300),
(703, '黑色 家用款', 1999.00, 50),
-- 母婴规格
(801, '900g', 299.00, 400),
(802, 'S码 100片', 159.00, 600),
-- 宠物规格
(901, '10kg', 159.00, 300),
(902, '10kg', 159.00, 350);

-- 6.6 插入测试地址
INSERT INTO `address` (`link_user`, `link_address`, `link_phone`, `user_id`) VALUES
('张三', '北京市朝阳区xxx街道', '13333333333', 4),
('张三', '上海市浦东新区xxx路', '15555555555', 4),
('测试用户', '广东省深圳市南山区xxx科技园', '13800138003', 4);

-- 6.7 插入轮播图
INSERT INTO `carousel` (`good_id`, `show_order`) VALUES
(301, 1),
(311, 2),
(331, 3),
(101, 4),
(201, 5);

-- 6.8 插入测试评论
INSERT INTO `review` (`id`, `good_id`, `user_id`, `content`, `rating`, `like_count`, `status`, `create_time`, `tags`) VALUES
(1, 301, 4, '手机运行速度很快，拍照效果也很好，非常满意！5G信号稳定，续航也不错。', 5, 245, 1, '2024-03-15 10:00:00', '速度快，拍照好，续航不错'),
(2, 301, 4, '性能强劲，玩游戏很流畅，散热也不错，推荐购买！', 5, 189, 1, '2024-03-16 11:00:00', '性能好，流畅，散热好'),
(3, 301, 4, '屏幕清晰，手感不错，这个价格很划算。', 4, 156, 1, '2024-03-17 14:00:00', '屏幕清晰，性价比高'),
(4, 311, 4, '笔记本很轻薄，性能出色，办公学习完全够用。', 5, 167, 1, '2024-03-18 09:00:00', '轻薄，性能好，办公必备'),
(5, 311, 4, '屏幕显示效果很好，键盘手感舒适，续航给力。', 5, 145, 1, '2024-03-19 16:00:00', '屏幕好，键盘舒适，续航长'),
(6, 331, 4, '耳机降噪效果很好，音质也不错，佩戴舒适。', 5, 234, 1, '2024-03-20 10:00:00', '降噪好，音质优秀，佩戴舒适'),
(7, 331, 4, '蓝牙连接稳定，续航时间长，性价比很高。', 5, 198, 1, '2024-03-21 11:00:00', '连接稳定，续航长，性价比高'),
(8, 101, 4, '卫衣质量很好，面料舒适，尺码标准，物流也快！', 5, 128, 1, '2024-03-22 14:00:00', '质量好，舒适，物流快'),
(9, 101, 4, '颜色很正，做工精细，这个价格真的很值！', 5, 95, 1, '2024-03-23 15:00:00', '做工精细，性价比高'),
(10, 201, 4, '鞋子很轻便，跑步很舒服，减震效果也不错。', 5, 156, 1, '2024-03-24 10:00:00', '轻便，舒适，减震好');

-- 6.9 插入评论回复
INSERT INTO `review_reply` (`review_id`, `user_id`, `content`, `like_count`, `status`) VALUES
(1, 2, '感谢您的好评！祝您使用愉快！', 45, 1),
(4, 2, '感谢支持！这款笔记本确实非常适合办公学习！', 67, 1),
(6, 2, '谢谢亲的认可！这款耳机的降噪效果确实是行业标杆！', 52, 1),
(8, 2, '感谢您的认可！卫衣的品質我们一直严格把控！', 78, 1);

-- 6.10 更新商品评论统计
UPDATE `good` SET 
  `review_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `status` = 1),
  `good_rating` = (SELECT IFNULL(AVG(`rating`), 5.00) FROM `review` WHERE `good_id` = `good`.`id` AND `status` = 1),
  `rating_5_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `rating` = 5 AND `status` = 1),
  `rating_4_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `rating` = 4 AND `status` = 1),
  `rating_3_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `rating` = 3 AND `status` = 1),
  `rating_2_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `rating` = 2 AND `status` = 1),
  `rating_1_count` = (SELECT COUNT(*) FROM `review` WHERE `good_id` = `good`.`id` AND `rating` = 1 AND `status` = 1)
WHERE `id` IN (SELECT DISTINCT `good_id` FROM `review`);

-- ============================================
-- 7. 完成提示
-- ============================================
SELECT '============================================' AS '';
SELECT '数据库初始化完成！' AS '';
SELECT '============================================' AS '';
SELECT '数据库名称：db_mall' AS '';
SELECT '测试用户：testuser / 123456' AS '';
SELECT '管理员：admin / 123456' AS '';
SELECT '客服账号：service001 / service002 / 123456' AS '';
SELECT '============================================' AS '';
