package com.easymusic.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;

@Component("springContext")
public final class SpringContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    //当容器初始化时，会调用这个方法，将applicationContext设置到静态变量中
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringContext.applicationContext == null) {
            SpringContext.applicationContext = applicationContext;
        }
    }

    public static Object getBean(String beanName) {
        return SpringContext.applicationContext.getBean(beanName);
    }

}