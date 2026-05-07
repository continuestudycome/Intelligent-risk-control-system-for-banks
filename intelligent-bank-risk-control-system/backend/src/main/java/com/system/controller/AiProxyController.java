package com.system.controller;

import com.system.common.Result;
import com.system.feign.AiServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-proxy")
@RequiredArgsConstructor
public class AiProxyController {

    private final AiServiceClient aiServiceClient;

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("backend-service ok");
    }

    @GetMapping("/hello")
    public Result<Object> hello(@RequestParam(name = "name", defaultValue = "bank-user") String name) {
        return Result.success(aiServiceClient.sayHello(name));
    }
}
