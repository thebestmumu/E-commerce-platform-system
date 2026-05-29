-- AI 对话历史表
CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `title` VARCHAR(200) DEFAULT '新对话' COMMENT '对话标题',
    `type` VARCHAR(50) DEFAULT 'chat' COMMENT '对话类型：chat(聊天)/search(搜索)/recommend(推荐)/order(订单)',
    `messages` JSON COMMENT '对话消息列表（JSON 格式）',
    `last_message_time` DATETIME COMMENT '最后一条消息时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史表';
