package org.goorm.wordsketch.rank.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements
    AuthenticationEntryPoint {

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authenticationException) {

    // Spring Security 필터 과정에서 발생한 예외를 @ControllerAdvice로 처리 위임
    handlerExceptionResolver.resolveException(request, response, null,
        (Exception) request.getAttribute("FilterException"));
  }
}
