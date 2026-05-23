package com.easymusic.entity.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AppConfig {

    @Value("${project.folder:}")
    private String projectFolder;

    @Value("${admin.account:admin}")
    private String adminAccount;

    @Value("${admin.password:123456}")
    private String adminPassword;

    @Value("${tianpuyue.api.key:}")
    private String tianpuyueApiKey;

    @Value("${tianpuyue.api.domain:https://api.tianpuyue.cn}")
    private String tianpuyueApiDomain;

    @Value("${web.domain:}")
    private String webDomain;

    @Value("${tianpuyue.api.courseOrderId:}")
    private String tianpuyueApiCourseOrderId;

    @Value("${auto.checkMusic:false}")
    private Boolean autoCheckMusic;

}


