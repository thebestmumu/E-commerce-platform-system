package com.rabbiter.em.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.constants.RedisConstants;
import com.rabbiter.em.entity.User;
import com.rabbiter.em.service.UserService;
import com.rabbiter.em.utils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 客服登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/service")
public class ServiceAuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 客服登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // 查询用户
            User user = userService.getByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 验证密码（实际应该用加密后的密码对比）
            // 这里简化处理，直接对比明文密码
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            queryWrapper.eq("password", password);
            User loginUser = userService.getOne(queryWrapper);
            
            if (loginUser == null) {
                return Result.error("密码错误");
            }

            // 检查是否是客服角色
            if (!"service".equals(loginUser.getRole())) {
                return Result.error("该账号不是客服账号，无权限访问");
            }

            // 生成 Token
            String token = TokenUtils.genToken(loginUser.getId().toString(), loginUser.getUsername());
            
            // 将 token 存储到 Redis 中
            redisTemplate.opsForValue().set(RedisConstants.USER_TOKEN_KEY + token, loginUser, RedisConstants.USER_TOKEN_TTL, TimeUnit.MINUTES);

            // 返回用户信息和 Token
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getId());
            data.put("username", loginUser.getUsername());
            data.put("nickname", loginUser.getNickname());
            data.put("role", loginUser.getRole());
            data.put("avatarUrl", loginUser.getAvatarUrl());

            return Result.ok(data, "登录成功");
        } catch (Exception e) {
            log.error("客服登录失败", e);
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 客服信息
     */
    @GetMapping("/info")
    public Result getInfo(@RequestHeader("serviceId") Long serviceId) {
        try {
            User user = userService.getById(serviceId.intValue());
            if (user == null) {
                return Result.error("用户不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("role", user.getRole());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("email", user.getEmail());
            data.put("phone", user.getPhone());

            return Result.ok(data);
        } catch (Exception e) {
            log.error("获取客服信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
}
