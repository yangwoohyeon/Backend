package com.core.halpme.common.config;

import com.core.halpme.api.members.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT 인증 필터 의존성 주입
    private final JwtAuthenticationFilter jwtFilter;

    // 비밀번호 암호화를 위한 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt 방식 사용
    }

    // Spring Security 설정 메인 메서드
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .sessionManagement(config -> config
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",                      // 회원/인증 엔드포인트  (개발 환경 임시)
                                "/api/v1/posts/**",                     // 봉사 신청글 엔드포인트 (개발 환경 임시)
                                "/api/v1/members/**",                   // 회원 조회 관련 엔드포인트
                                "/api/v1/s3/**"                         // S3 관련 엔드포인트
                        ).permitAll()
                        .requestMatchers(
                                "/",                                    // 인덱스
                                "/login",                               // 로그인 폼
                                "/signup",                              // 회원가입
                                "/css/**", "/js/**", "/images/**",  // 정적 리소스
                                "/favicon.ico",                         // 파비콘
                                "/error",                               // 에러 페이지
                                "/chat/inbox/**","/ws/**"             // 채팅, 웹소켓
                        ).permitAll()
                        .requestMatchers(
                                // Swagger UI
                                "/v3/api-docs/**",
                                "/api/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api/swagger-ui/**",
                                "/swagger-resources/**",
                                "/api/swagger-resources/**",
                                "/api/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated() // 나머지 요청은 인증 필요
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
                .build(); // SecurityFilterChain 반환
    }
}