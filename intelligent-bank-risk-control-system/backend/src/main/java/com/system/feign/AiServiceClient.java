package com.system.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ai-service")
public interface AiServiceClient {

    @GetMapping("/")
    String getRoot();

    @GetMapping("/hello/{name}")
    String sayHello(@PathVariable("name") String name);
}
