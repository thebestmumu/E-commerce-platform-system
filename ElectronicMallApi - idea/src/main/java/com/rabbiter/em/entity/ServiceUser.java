package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服用户实体类（从 User 表扩展）
 */
@Data
@TableName("sys_user")
public class ServiceUser {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    @TableField(exist = false)
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatarUrl;

    /**
     * 用户角色：customer(普通用户)/service(客服)/admin(管理员)
     */
    private String role;

    /**
     * 是否激活
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 客服专属字段：工号
     */
    @TableField(exist = false)
    private String serviceNo;

    /**
     * 客服专属字段：当前状态（online/offline/busy）
     */
    @TableField(exist = false)
    private String status;

    /**
     * 客服专属字段：已分配工单数
     */
    @TableField(exist = false)
    private Integer assignedTicketsCount;
}
