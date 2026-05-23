package com.easymusic.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/test")
public class TestController {

    private String key = "key";
    @RequestMapping("/save")
    public String save(HttpSession session){
        String msg="11111111111111111111";
        if(msg==null) return "msg不能为空";

        session.setAttribute(key,msg);

        return "保存session信息成功"+session.getId();
    }

    @RequestMapping("/get")
    public String get(HttpSession session){


        return "获取信息成功"+session.getAttribute(key);
    }
}
