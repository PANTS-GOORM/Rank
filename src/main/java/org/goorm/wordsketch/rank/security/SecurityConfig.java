package org.goorm.wordsketch.rank.security;

import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.goorm.wordsketch.rank.security.jwt.JwtAuthenticationProcessingFilter;
import org.goorm.wordsketch.rank.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * <pre>
* 인증은 CustomJsonUsernamePasswordAuthenticationFilter에서 authenticate()로 인증된
사용자로 처리
* JwtAuthenticationProcessingFilter는 AccessToken, RefreshToken 재발급
 * </pre>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtService jwtService;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
        // FormLogin 사용 안함
        .formLogin(AbstractHttpConfigurer::disable)
        // httpBasic 사용 안함
        .httpBasic(AbstractHttpConfigurer::disable)
        // Token 기반 인증 방식이기에 csrf 보안 사용 안함
        .csrf(AbstractHttpConfigurer::disable)
        // 세션을 사용하지 않으므로 STATELESS로 설정
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 모든 URL에 대해 인증 필수 설정
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/rank-ws").permitAll()
            .anyRequest().authenticated())
        // Cors 설정
        .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource()))
        // 필터 도중에 발생하는 예외를 처리하는 커스텀 컴포넌트 등록
        .exceptionHandling(handler -> handler.authenticationEntryPoint(customAuthenticationEntryPoint))
        // 원래 스프링 시큐리티 필터 순서가 LogoutFilter 이후에 로그인 필터 동작
        // 따라서, LogoutFilter 이후에 커스텀 필터가 동작하도록 설정
        // 순서 : LogoutFilter -> JwtAuthenticationProcessingFilter
        .addFilterAfter(jwtAuthenticationProcessingFilter(), LogoutFilter.class);

    return http.build();
  }

  /**
   * Cors 관련 설정
   *
   * 헤더와 HTTP Method는 모두 허용
   * Origin은 로컬 개발환경과 배포환경의 Origin만 허용
   *
   * @return Cors 설정 객체
   */
  CorsConfigurationSource corsConfigurationSource() {
    return request -> {
      CorsConfiguration corsConfiguration = new CorsConfiguration();
      corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
      corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
      corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("http://localhost:3000"));
      corsConfiguration.setAllowCredentials(true);
      corsConfiguration.setMaxAge(3600L);
      return corsConfiguration;
    };
  }

  @Bean
  public JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter() {

    JwtAuthenticationProcessingFilter jwtAuthenticationProcessingFilter = new JwtAuthenticationProcessingFilter(
        jwtService);

    return jwtAuthenticationProcessingFilter;
  }
}