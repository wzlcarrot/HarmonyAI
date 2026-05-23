package com.easymusic.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置类
 * 用于布隆过滤器历史数据同步任务
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    /**
     * 布隆过滤器加载器线程池
     * 用于夜间一致性校准任务
     */
    @Bean("bloomFilterLoaderExecutor")
    public ThreadPoolExecutor bloomFilterLoaderExecutor() {
        int corePoolSize = 5;
        int maximumPoolSize = 10;
        long keepAliveTime = 60L;
        int queueCapacity = 100;

        return new ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "bloom-filter-loader-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy() // 降级策略：线程池满时，调用者执行
        );
    }
}



