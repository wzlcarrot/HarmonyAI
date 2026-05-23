package com.easymusic.aspect;

import com.easymusic.annotation.GlobalInterceptor;
import com.easymusic.entity.dto.TokenUserInfoDTO;
import com.easymusic.entity.enums.ResponseCodeEnum;
import com.easymusic.exception.BusinessException;
import com.easymusic.redis.RedisComponent;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Component("operationAspect")
@Aspect
@Slf4j

//首先定义一个切面
public class GlobalOperationAspect {

    @Resource
    private RedisComponent redisComponent;

    @Before("@annotation(com.easymusic.annotation.GlobalInterceptor)")  //在GlobalInterceptor注解的方法之前执行这个方法
    public void interceptorDo(JoinPoint point) {
        Method method = ((MethodSignature) point.getSignature()).getMethod();   //得到了注解下的方法
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class); //获得注解
        if (interceptor==null) {
            return;
        }

        if (interceptor.checkLogin()) {
            checkLogin();
        }
    }

    //校验登录
    private void checkLogin() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("token");
        TokenUserInfoDTO tokenUserInfoDto = redisComponent.getTokenUserInfoDTO(token);

        if (tokenUserInfoDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }

}