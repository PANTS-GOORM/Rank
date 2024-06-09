package org.goorm.wordsketch.rank.acceptance.rankroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.goorm.wordsketch.rank.acceptance.global.AcceptanceTest;
import org.goorm.wordsketch.rank.ranklobby.RankLobbyService;
import org.goorm.wordsketch.rank.ranklobby.RankRoom;
import org.goorm.wordsketch.rank.rankroom.RankRoomService;
import org.goorm.wordsketch.rank.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestConstructor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AcceptanceTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@DisplayName("경쟁 학습 방 입장 API 인수테스트")
public class RankRoomEntranceApiTest {

  private final JwtService jwtService;
  private final RankRoomService rankRoomService;
  private final RankLobbyService rankLobbyService;

  @LocalServerPort
  private int portNum;

  @Nested
  @DisplayName("Given: 서버에 학습방이 있을 때,")
  class Given_서버에_학습방이_있을_때 {

    RankRoom createdRankRoom = rankLobbyService.registRankRoom(RankRoom.builder()
        .roomUUID(UUID.randomUUID().toString())
        .roomName("접속 테스트 방")
        .build());

    @Nested
    @DisplayName("When: 올바른 accessToken과 함께 roomUUID로 참여를 요청하면,")
    class 올바른_accessToken과_함께_roomUUID로_참여를_요청하면 {

      private String stompUrl;
      private String testUserEmail;
      private String testAccessToken;
      private StompSession stompSession;

      올바른_accessToken과_함께_roomUUID로_참여를_요청하면() throws InterruptedException, ExecutionException, TimeoutException {

        // Stomp 테스트를 위한 클라언트 객체 생성
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        // 테스트 유저용 testAccessToken 발행
        testUserEmail = "test@email.com";
        testAccessToken = jwtService.createAccessTokenForTest(testUserEmail);
        log.info(testAccessToken);

        // Stomp Filter 인증을 위해 헤더 설정
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add("AccessToken", testAccessToken);

        // connect()가 완료되고 stompSession를 반환할 때까지 대기
        // 2초가 지나면 TimeOutException 발생
        stompUrl = "ws://localhost:" + portNum + "/rank-ws";
        stompSession = stompClient
            .connectAsync(stompUrl, new WebSocketHttpHeaders(), stompHeaders, new StompSessionHandlerAdapter() {
            }).get(2, TimeUnit.SECONDS);
      }

      @Test
      @DisplayName("Then: 학습방 채팅에 참여할 수 있다.")
      void Then_학습방_채팅에_참여할_수_있다() throws InterruptedException, ExecutionException, TimeoutException {

        // 테스트 쓰레드가 먼저 꺼지는 걸 방지하기 위해, CompletableFuture를 사용
        CompletableFuture<String> subscribeFuture = new CompletableFuture<>();

        // Stomp 메세지 구독 등록
        String testRoomUUID = createdRankRoom.getRoomUUID();
        String subUrl = "/sub/rankroom/" + testRoomUUID;
        stompSession.subscribe(subUrl, new StompFrameHandler() {

          // 메세지 타입 지정
          @SuppressWarnings("null")
          @Override
          public Type getPayloadType(StompHeaders headers) {
            return String.class;
          }

          // 메세지 파싱
          @SuppressWarnings("null")
          @Override
          public void handleFrame(StompHeaders headers, Object payload) {

            subscribeFuture.complete(String.valueOf(payload));
          }
        });

        // 테스트 채팅 메세지 발행
        String testPubMessage = "메세지 발송 확인 테스트 메세지 입니다.";
        String pubUrl = "/pub/rankroom/" + testRoomUUID;
        stompSession.send(pubUrl, testPubMessage);

        // 값이 들어올때 까지 최대 3초간 기다림
        String subscribedMessage = subscribeFuture.get(3, TimeUnit.SECONDS);

        // 메세지가 일치하는지 확인
        assertEquals(testPubMessage, subscribedMessage);

        // 현재 유저 위치가 구독한 roomUUID 인지 확인
        assertEquals(testRoomUUID, rankRoomService.getUserLocation(testUserEmail).getRoomUUID());

        // 세션 종료 후, 해당 유저의 위치 확인
        stompSession.disconnect();
        Thread.sleep(100);

        assertThrows(ResponseStatusException.class,
            () -> rankRoomService.getUserLocation(testUserEmail));
      }
    }

    @Nested
    @DisplayName("When: 올바르지 않은 accessToken과 함께 방 참여를 요청하면,")
    class When_올바르지_않은_accessToken과_함께_방_참여를_요청하면 {

      private WebSocketStompClient stompClient;
      private String stompUrl;
      private StompHeaders stompHeaders;

      When_올바르지_않은_accessToken과_함께_방_참여를_요청하면() {

        // Stomp 테스트를 위한 클라언트 객체 생성
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        // 테스트 유저용 testAccessToken 발급
        String testAccessToken = "InvalidAccessToken";

        // Stomp Filter 인증을 위해 헤더 설정
        stompHeaders = new StompHeaders();
        stompHeaders.add("AccessToken", testAccessToken);

        // connect()가 완료되고 stompSession를 반환할 때까지 대기
        // 2초가 지나면 TimeOutException 발생
        stompUrl = "ws://localhost:" + portNum + "/rank-ws";
      }

      @Test
      @DisplayName("에러코드 401을 메세지와 함께 반환한다.")
      void 에러코드_401을_메세지와_함께_반환한다() throws InterruptedException, ExecutionException, TimeoutException {

        stompClient
            .connectAsync(stompUrl, new WebSocketHttpHeaders(), stompHeaders, new StompSessionHandlerAdapter() {

              @SuppressWarnings("null")
              @Override
              public void handleTransportError(StompSession stompSession, Throwable throwable) {

                log.info("에러 확인");
                log.info("Error -- " + throwable);
              }

            }).get(2, TimeUnit.SECONDS);
      }
    }

    @Nested
    @DisplayName("When: accessToken 없이 방 참여를 요청하면,")
    class When_accessToken_없이_방_참여를_요청하면 {

      private WebSocketStompClient stompClient;
      private String stompUrl;
      private StompHeaders stompHeaders;

      When_accessToken_없이_방_참여를_요청하면() throws InterruptedException, ExecutionException, TimeoutException {

        // Stomp 테스트를 위한 클라언트 객체 생성
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        // 테스트 유저용 testAccessToken 발급
        String testAccessToken = "InvalidAccessToken";

        // Stomp Filter 인증을 위해 헤더 설정
        stompHeaders = new StompHeaders();
        stompHeaders.add("AccessToken", testAccessToken);

        // connect()가 완료되고 stompSession를 반환할 때까지 대기
        // 2초가 지나면 TimeOutException 발생
        stompUrl = "ws://localhost:" + portNum + "/rank-ws";
      }

      @Test
      @DisplayName("에러코드 400을 메세지와 함께 반환한다.")
      void 에러코드_400을_메세지와_함께_반환한다() throws InterruptedException, ExecutionException, TimeoutException {

        // assertThrows(ResponseStatusException.class, () -> stompClient
        // .connectAsync(stompUrl, new WebSocketHttpHeaders(), stompHeaders, new
        // StompSessionHandlerAdapter() {
        // }).get(2, TimeUnit.SECONDS));
        CompletableFuture<StompSession> session = stompClient
            .connectAsync(stompUrl, new WebSocketHttpHeaders(), stompHeaders, new StompSessionHandlerAdapter() {

              @SuppressWarnings("null")
              @Override
              public void handleException(StompSession session, @Nullable StompCommand command,
                  StompHeaders headers, byte[] payload, Throwable exception) {

                log.info("HEREEE");
              }

              @SuppressWarnings("null")
              @Override
              public void handleTransportError(StompSession stompSession, Throwable throwable) {

                log.info("Err:");
                log.info(throwable.getCause().getLocalizedMessage());
              }
            });
      }
    }
  }
}
