package com.easymusic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication(scanBasePackages = {"com.easymusic"})
@MapperScan(basePackages = {"com.easymusic.mappers"})
@EnableTransactionManagement
@EnableScheduling   //开启定时任务
@EnableAsync    //开启异步任务
public class EasyMusicWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyMusicWebApplication.class, args);
    }

}
