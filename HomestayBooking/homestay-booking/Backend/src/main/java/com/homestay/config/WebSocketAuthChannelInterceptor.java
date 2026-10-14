package com.homestay.config;

import com.homestay.entity.User;
import com.homestay.repository.UserRepository;
import com.homestay.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/** FR-15 — JWT on STOMP CONNECT (Authorization: Bearer or nativeHeaders token). */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public WebSocketAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth == null || auth.isBlank()) {
            auth = accessor.getFirstNativeHeader("authorization");
        }
        if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new IllegalArgumentException("WebSocket CONNECT requires Bearer token");
        }
        String token = auth.substring(7).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid JWT for WebSocket");
        }
        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new IllegalArgumentException("User inactive");
        }
        String role = "ROLE_" + user.getRole().name();
        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority(role))));
        return message;
    }
}
