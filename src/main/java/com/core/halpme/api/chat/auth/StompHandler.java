package com.core.halpme.api.chat.auth;

import com.core.halpme.api.members.jwt.JwtTokenProvider;
import com.core.halpme.common.exception.BaseException;
import com.core.halpme.common.response.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompHandler implements ChannelInterceptor { //ChannelInterceptor를 이용해 STOMP 메시지를 가로쳄

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) { //첫 연결할때 CONNECT 프레임 전송 시 JWT 토큰 유효성 검사
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            log.info("WebSocket connect Authorization header: {}", authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new BaseException(ErrorStatus.UNAUTHORIZED_EMPTY_TOKEN.getHttpStatus(), "헤더 없음");
            }

            String token = authHeader.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new BaseException(ErrorStatus.UNAUTHORIZED_INVALID_TOKEN.getHttpStatus(), "토큰 무효");
            }

            String email = jwtTokenProvider.getEmail(token);
            StompPrincipal principal = new StompPrincipal(email);

            //setUser에 principal만 설정
            accessor.setUser(principal);

            //세션에도 저장
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                sessionAttributes = new HashMap<>();
                accessor.setSessionAttributes(sessionAttributes);
            }
            sessionAttributes.put("user", principal);
        }

        return message;
    }



}
