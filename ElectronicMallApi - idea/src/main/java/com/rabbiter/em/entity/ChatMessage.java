package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单 ID（关联 ticket 表）
     */
    private Long ticketId;

    /**
     * 发送者 ID（用户或客服）
     */
    private Long senderId;

    /**
     * 发送者角色：user(用户)/service(客服)
     */
    private String senderRole;

    /**
     * 接收者 ID
     */
    private Long receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型：text(文本)/image(图片)/file(文件)/system(系统消息)
     */
    private String messageType;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 读取时间
     */
    private LocalDateTime readAt;
}
