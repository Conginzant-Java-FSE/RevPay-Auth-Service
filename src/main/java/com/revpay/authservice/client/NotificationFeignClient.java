package com.revpay.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "notification-service",
        fallback = NotificationFeignClientFallback.class
)
public interface NotificationFeignClient {

    @PostMapping("/internal/notifications/create")
    void createNotification(@RequestBody Map<String, Object> payload);
}
