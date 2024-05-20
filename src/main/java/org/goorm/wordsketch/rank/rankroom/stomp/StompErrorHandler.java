package org.goorm.wordsketch.rank.rankroom.stomp;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StompErrorHandler extends StompSubProtocolErrorHandler {

  @SuppressWarnings("null")
  @Override
  public Message<byte[]> handleClientMessageProcessingError(
      Message<byte[]> message,
      Throwable throwable) {

    if ("올바르지 않은 accessToken 입니다.".equals(throwable.getMessage())) {

      log.info("에러 발생");
      return errorMessage("유저 인증에 실패하여 연결에 실패했습니다.");
    }

    return super.handleClientMessageProcessingError(message, throwable);
  }

  private Message<byte[]> errorMessage(String errorMessage) {

    StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
    stompHeaderAccessor.setLeaveMutable(true);

    return MessageBuilder.createMessage(errorMessage.getBytes(StandardCharsets.UTF_8),
        stompHeaderAccessor.getMessageHeaders());
  }
}
