package com.incident.bff.config;

import com.incident.bff.redis.RedisMessageRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;

import java.util.List;

@Configuration
public class RedisSubscriberConfig {

    @Bean
    public ReactiveRedisMessageListenerContainer redisMessageListenerContainer(
            ReactiveRedisConnectionFactory connectionFactory,
            RedisMessageRelay messageRelay) {
        
        ReactiveRedisMessageListenerContainer container = new ReactiveRedisMessageListenerContainer(connectionFactory);
        
        container.receive(
                new ChannelTopic("alerts-live"),
                new ChannelTopic("analysis-live"),
                new ChannelTopic("traces-live")
        ).subscribe(message -> {
            String channel = message.getChannel();
            String payload = message.getMessage();
            messageRelay.relayMessage(channel, payload);
        });
        
        return container;
    }
}
