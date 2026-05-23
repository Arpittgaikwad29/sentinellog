package com.sentinellog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration.
 *
 * Clients connect to /ws, then subscribe to:
 *   /topic/logs    — raw log stream
 *   /topic/alerts  — triggered alerts
 *   /topic/stats   — rolling dashboard stats
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for broadcasting to subscribers
        registry.enableSimpleBroker("/topic");
        // Prefix for messages sent FROM clients (not used in this app but good practice)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")           // Raw WebSocket endpoint
                .setAllowedOriginPatterns("*")
                .withSockJS();                // Fallback for older browsers
    }
}
