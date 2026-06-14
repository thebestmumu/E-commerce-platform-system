package com.rabbiter.em.exception;

import com.rabbiter.em.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/*
全局异常处理
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public Result handle(ServiceException se){
        return Result.error(se.getCode(),se.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseBody
    public Result handleRateLimitException(RateLimitException e) {
        log.warn("限流异常：{}", e.getMessage());
        return Result.error("429", e.getMessage());
    }
}
