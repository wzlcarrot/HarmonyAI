package com.easymusic.aspect;

import com.easymusic.annotation.RateLimit;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.enums.LimitType;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 基于 Redisson 的分布式限流切面。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;
    private final RedisComponent redisComponent;

    //before和around注解就是类似于保安和警察，，，进去的时候保安不允许搜身，但是警察可以，如果查到违禁品则立马扣留。
    @Around("@annotation(rateLimit)")
    public Object doLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getRequest();
        String key = buildKey(rateLimit, request);

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 仅首次设置速率，避免并发重置。
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.interval(), RateIntervalUnit.SECONDS);

        boolean acquired = rateLimiter.tryAcquire();
        if (!acquired) {
            log.warn("rate limit hit, key={}", key);
            throw new BusinessException("请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException("无法获取请求上下文");
        }
        return attributes.getRequest();
    }

    private String buildKey(RateLimit rateLimit, HttpServletRequest request) {
        StringBuilder builder = new StringBuilder("easymusic:rl:").append(rateLimit.name()).append(":");
        LimitType type = rateLimit.limitType();
        switch (type) {
            case USER:
                String token = request.getHeader("token");
                TokenUserInfoDTO userInfoDTO = redisComponent.getTokenUserInfoDTO(token);
                String userId = userInfoDTO == null ? "anonymous" : userInfoDTO.getUserId();
                builder.append("user:").append(userId);
                break;
            case IP:
                builder.append("ip:").append(getClientIp(request));
                break;
            default:
                builder.append("global");
        }
        return builder.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = new String[]{"X-Forwarded-For", "X-Real-IP"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

