package com.rabbiter.em.annotation;

import java.lang.annotation.*;

/**
 * 速率限制注解
 * 用于限制接口访问频率
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键的前缀
     */
    String prefix() default "rate_limit";

    /**
     * 限流键表达式，支持 SpEL，如 "#userId"、"#request.remoteAddr"
     */
    String key() default "";

    /**
     * 最大请求次数
     */
    int maxRequests() default 60;

    /**
     * 时间窗口（秒）
     */
    int windowSeconds() default 60;

    /**
     * 超过限制时的提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}
