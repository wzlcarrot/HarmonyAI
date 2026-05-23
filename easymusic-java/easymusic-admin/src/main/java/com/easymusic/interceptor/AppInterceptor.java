package com.easymusic.interceptor;

import com.easymusic.entity.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
@Slf4j
public class AppInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查会话中是否有管理员登录信息
        HttpSession session = request.getSession();
        String adminUser = (String) session.getAttribute("adminUser");

        // 如果会话中没有管理员信息，重定向到登录页面
        if (adminUser == null) {
            // AJAX请求返回JSON
            String requestType = request.getHeader("X-Requested-With");
            if ("XMLHttpRequest".equals(requestType)) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":901,\"msg\":\"未登录\",\"data\":null}");
            } else {

                // 普通请求重定向到登录页面
                response.sendRedirect("/login");
            }
            return false;
        }

        // 如果已登录，允许访问
        return true;
    }
}
