package com.revpay.authservice.client;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationFeignClientFallback implements NotificationFeignClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationFeignClientFallback.class);

    @Override
    public void createNotification(Map<String, Object> payload) {
        logger.warn("notification-service is unavailable. Notification not sent. Payload: {}", payload);
    }
}
