package com.library.common.web;

import com.library.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 根路径探针：直接访问 http://localhost:8080/ 时给出可读的运行信息，
 * 避免新人误把后端端口当页面地址访问，看到的是一片 401/404。
 */
@RestController
public class RootController {

    @Value("${server.port:8080}")
    private int port;

    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("应用", "图书管理系统 API");
        info.put("状态", "running");
        info.put("前端页面", "http://localhost:5173  ← 页面请访问这个地址");
        info.put("登录接口", "POST /api/auth/login  {\"username\":\"admin\",\"password\":\"admin123\"}");
        info.put("接口前缀", "/api");
        info.put("本服务端口", port);
        return Result.ok(info);
    }
}
