package com.rabbiter.em.interceptor;

import com.rabbiter.em.annotation.RateLimit;
import com.rabbiter.em.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 速率限制拦截器
 * 基于 Redis 滑动窗口算法实现
 */
@Slf4j
@Aspect
@Component
public class RateLimitInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * SpEL 表达式解析器
     */
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    
    /**
     * 参数名发现器
     */
    private static final ParameterNameDiscoverer PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.rabbiter.em.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // 生成限流 key
        String key = generateKey(rateLimit, method, joinPoint.getArgs());
        
        // 获取当前时间戳（秒）
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long windowStart = currentTimestamp - rateLimit.windowSeconds();

        // 使用 Redis 事务实现滑动窗口计数
        String redisKey = rateLimit.prefix() + ":" + key;
        
        // 删除窗口外的数据
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
        
        // 获取窗口内的请求数
        Long count = redisTemplate.opsForZSet().zCard(redisKey);
        
        if (count != null && count >= rateLimit.maxRequests()) {
            log.warn("限流触发 - key: {}, 当前请求数：{}/{}", 
                    redisKey, count, rateLimit.maxRequests());
            throw new RateLimitException(rateLimit.message());
        }

        // 添加当前请求到窗口
        redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTimestamp), currentTimestamp);
        redisTemplate.expire(redisKey, rateLimit.windowSeconds(), TimeUnit.SECONDS);

        log.debug("限流检查通过 - key: {}, 当前请求数：{}/{}", 
                redisKey, count != null ? count + 1 : 1, rateLimit.maxRequests());

        return joinPoint.proceed();
    }

    /**
     * 生成限流 key
     */
    private String generateKey(RateLimit rateLimit, Method method, Object[] args) {
        // 如果指定了 key 表达式，使用 SpEL 解析
        if (!rateLimit.key().isEmpty()) {
            // 获取参数名
            String[] parameterNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
            
            // 创建 SpEL 上下文
            EvaluationContext context = new StandardEvaluationContext();
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 解析表达式
            try {
                Expression expression = PARSER.parseExpression(rateLimit.key());
                Object value = expression.getValue(context);
                if (value != null) {
                    return value.toString();
                }
            } catch (Exception e) {
                log.warn("SpEL 解析失败：{}, 使用默认 key", rateLimit.key(), e);
            }
        }

        // 默认使用 IP 地址作为 key
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getRemoteAddr();
            return "ip:" + ip;
        }

        return "default";
    }
}
