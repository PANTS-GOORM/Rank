package org.goorm.wordsketch.rank.rankroom.stomp;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompErrorHandler stompErrorHandler;
  private final FilterChannelInterceptor filterChannelInterceptor;

  @SuppressWarnings("null")
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {

    config.enableSimpleBroker("/sub");
    config.setApplicationDestinationPrefixes("/pub", "/sub");
  }

  @SuppressWarnings("null")
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {

    // 로컬 개발 환경과 배포 환경을 위한 CORS 설정
    registry.addEndpoint("/rank-ws").setAllowedOrigins("http://localhost:3000", "https://wordsketch.site/");

    // 커스텀 에러 핸들러 설정
    registry.setErrorHandler(stompErrorHandler);
  }

  @SuppressWarnings("null")
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {

    registration.interceptors(filterChannelInterceptor);
  }
}
