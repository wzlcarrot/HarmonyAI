package com.easymusic.annotation;

import com.easymusic.entity.enums.LimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式限流注解，基于 Redisson 的 RRateLimiter。
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 前缀，用于区分业务场景。
     */
    String name();

    /**
     * 许可数（在 interval 时间窗口内允许的请求次数）。
     */
    long rate();

    /**
     * 时间窗口，单位：秒。
     */
    long interval();

    /**
     * 限流维度：全局 / 用户 / IP。
     */
    LimitType limitType() default LimitType.GLOBAL;
}

