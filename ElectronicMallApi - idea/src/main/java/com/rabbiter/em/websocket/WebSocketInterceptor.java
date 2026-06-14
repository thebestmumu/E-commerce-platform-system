package com.rabbiter.em.websocket;

import cn.hutool.jwt.JWTUtil;
import com.rabbiter.em.constants.RedisConstants;
import com.rabbiter.em.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 用于验证 token 和获取用户信息
 */
@Slf4j
@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 从请求参数中获取 token
        String path = request.getURI().getPath();
        String queryString = request.getURI().getQuery();
        
        log.info("WebSocket 握手请求：path={}, query={}", path, queryString);
        
        if (queryString != null) {
            String[] params = queryString.split("&");
            String token = null;
            String role = null;
            
            for (String param : params) {
                String[] kv = param.split("=");
                if ("token".equals(kv[0])) {
                    token = kv[1];
                } else if ("role".equals(kv[0])) {
                    role = kv[1];
                }
            }
            
            if (token != null) {
                // 验证 token
                try {
                    // 从 Redis 中获取用户信息（使用正确的 key 前缀）
                    String userKey = RedisConstants.USER_TOKEN_KEY + token;
                    String userJson = redisTemplate.opsForValue().get(userKey);
                    
                    if (userJson != null) {
                        // 解析用户信息
                        cn.hutool.jwt.JWT jwtToken = JWTUtil.parseToken(token);
                        cn.hutool.jwt.JWTPayload payload = jwtToken.getPayload();
                        Object audObj = payload.getClaim("aud");
                        Long userId = Long.valueOf(audObj.toString());
                        
                        // 将用户信息放入 attributes，供 WebSocketHandler 使用
                        attributes.put("userId", userId);
                        attributes.put("role", role != null ? role : "user");
                        attributes.put("token", token);
                        
                        log.info("WebSocket 握手成功：userId={}, role={}", userId, role);
                        return true;
                    } else {
                        log.warn("Token 无效或已过期：{}", token);
                    }
                } catch (Exception e) {
                    log.error("Token 验证失败", e);
                }
            }
        }
        
        // 验证失败，拒绝握手
        log.warn("WebSocket 握手失败：token 无效");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后处理，可以留空
    }
}
