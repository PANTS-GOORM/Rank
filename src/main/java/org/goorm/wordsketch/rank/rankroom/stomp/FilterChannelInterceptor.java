package org.goorm.wordsketch.rank.rankroom.stomp;

import java.security.Principal;
import java.util.Optional;

import org.goorm.wordsketch.rank.rankroom.RankRoomService;
import org.goorm.wordsketch.rank.security.PasswordUtil;
import org.goorm.wordsketch.rank.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Component
@RequiredArgsConstructor
public class FilterChannelInterceptor implements ChannelInterceptor {

  private final JwtService jwtService;
  private final RankRoomService rankRoomService;
  private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

  @Value("${jwt.access.cookie}")
  private String accessCookie;

  @SuppressWarnings("null")
  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {

    StompHeaderAccessor stompHeaderAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    Assert.notNull(stompHeaderAccessor, "StompHeaderAccessor를 찾을 수 없습니다.");
    if (stompHeaderAccessor.getCommand() == StompCommand.CONNECT) {

      // 담겨있는 유저 userUUID가 유효한 지 검증 후 추출
      String accessToken = String.valueOf(stompHeaderAccessor.getNativeHeader(accessCookie).get(0));
      Optional<String> userEmail = jwtService.validateAccessToken(accessToken);

      // 확인된 userUUID 를 기반으로 세션 유저 설정
      userEmail.ifPresent((userUUID) -> {

        String password = PasswordUtil.generateRandomPassword();

        UserDetails userDetailsUser = User.builder()
            .username(userUUID)
            .password(password)
            .roles("USER")
            .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetailsUser, null,
            authoritiesMapper.mapAuthorities(userDetailsUser.getAuthorities()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        stompHeaderAccessor.setUser(authentication);
      });

    } else if (stompHeaderAccessor.getCommand() == StompCommand.SUBSCRIBE) {

      // stompHeaderAccessor 에서 roomUUID, 유저 userUUID 추출
      String roomUUID = stompHeaderAccessor.getDestination().split("/")[3];
      String userUUID = stompHeaderAccessor.getUser().getName();

      // null 값이 담겨있다면 잘못된 요청임을 알림
      if (roomUUID == null || userUUID == null)
        throw new MessageDeliveryException("요청 헤더의 값들이 올바르지 않습니다.");

      // roomUUID에 해당하는 RankRoom에 입장정보 업데이트
      rankRoomService.rankRoomEntrance(roomUUID, userUUID);

    } else if (stompHeaderAccessor.getCommand() == StompCommand.DISCONNECT) {

      // stompHeaderAccessor 에서 userUUID 추출
      Optional<Principal> user = Optional.ofNullable(stompHeaderAccessor.getUser());
      user.ifPresent((userDetails) -> {

        String userUUID = userDetails.getName();

        // 클라이언트와 StompSubProtocolMessageHandler, 둘 다 요청을 보낼 수 있으므로
        // 유저 위치를 재확인하는 로직을 포함하는 rankRoomExit 함수 호출
        rankRoomService.rankRoomExit(userUUID);
      });
    }
    return message;
  }
}