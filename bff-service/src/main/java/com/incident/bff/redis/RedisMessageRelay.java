package com.incident.bff.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageRelay {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageRelay.class);
    private final SimpMessagingTemplate messagingTemplate;

    public RedisMessageRelay(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void relayMessage(String channel, String payload) {
        String topic;
        switch (channel) {
            case "alerts:live":
                topic = "/topic/alerts";
                break;
            case "analysis:live":
                topic = "/topic/analysis";
                break;
            case "traces:live":
                topic = "/topic/traces";
                break;
            default:
                log.warn("Unknown Redis channel: {}", channel);
                return;
        }
        
        log.debug("Relaying message from Redis {} to STOMP {}", channel, topic);
        messagingTemplate.convertAndSend(topic, payload);
    }
}
