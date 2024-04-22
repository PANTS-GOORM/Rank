package org.goorm.wordsketch.rank.acceptance.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.goorm.wordsketch.rank.acceptance.global.AcceptanceTest;
import org.goorm.wordsketch.rank.ranklobby.RankLobbyService;
import org.goorm.wordsketch.rank.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import lombok.RequiredArgsConstructor;

@AcceptanceTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@DisplayName("경쟁 어휘 학습 방 생성 API 인수테스트")
public class RankRoomCreateApiTest {

  private final WebTestClient webTestClient;
  private final RankLobbyService rankLobbyService;
  private final JwtService jwtService;

  @Value("${jwt.access.cookie}")
  private String accessCookie;

  @Nested
  @DisplayName("Given: 서버에 방이 하나도 없을 때,")
  class Given_서버에_방이_하나도_없을_때 {

    // AcceptanceTestExecutionListener에서 매 테스트 전후로 DB를 초기화
    Given_서버에_방이_하나도_없을_때() {

      assertEquals(0, rankLobbyService.getAllRankRooms().size());
    }

    @Nested
    @DisplayName("When: 올바른 accessToken과 함께 지정한 방 이름으로 생성을 요청하면,")
    class When_올바른_accessToken과_함께_지정한_방_이름으로_생성을_요청하면 {

      private String accessToken = jwtService.createAccessTokenForTest("test@email.com");
      private String testRoomName = "테스트 방";

      ResponseSpec responseSpec = webTestClient
          .post()
          .uri("/rank/lobby/rankroom")
          .cookie(accessCookie, accessToken)
          .bodyValue(testRoomName)
          .exchange();

      @Test
      @DisplayName("Then: 서버에 요청한 방이 생성된다.")
      void Then_서버에_요청한_방이_생성된다() {

        responseSpec
            .expectStatus().isOk()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        // DB에도 생성되었는지 roomName을 기준으로 확인
        assertEquals(testRoomName, rankLobbyService.getRankRoom(testRoomName).getRoomName());
      }
    }

    @Nested
    @DisplayName("When: accessToken과 함께 지정한 방 이름없이 방 생성을 요청하면,")
    class When_accessToken과_함께_지정한_방_이름없이_방_생성을_요청하면 {

      private String accessToken = jwtService.createAccessTokenForTest("test@email.com");

      ResponseSpec responseSpec = webTestClient
          .post()
          .uri("/rank/lobby/rankroom")
          .cookie(accessCookie, accessToken)
          .exchange();

      @Test
      @DisplayName("에러코드 400을 메세지와 함께 반환한다.")
      void 에러코드_400을_메세지와_함께_반환한다() {

        String returnedServerResponse = responseSpec
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertEquals("요청에 필요한 값들을 찾을 수 없습니다.", returnedServerResponse);
      }
    }

    @Nested
    @DisplayName("When: 올바르지 않은 accessToken과 함께 방 생성을 요청하면,")
    class When_올바르지_않은_accessToken과_함께_방_생성을_요청하면 {

      private String accessToken = "InvalidAccessToken";

      ResponseSpec responseSpec = webTestClient
          .post()
          .uri("/rank/lobby/rankroom")
          .cookie(accessCookie, accessToken)
          .exchange();

      @Test
      @DisplayName("에러코드 401을 메세지와 함께 반환한다.")
      void 에러코드_401을_메세지와_함께_반환한다() {

        String returnedServerResponse = responseSpec
            .expectStatus().isUnauthorized()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertEquals("올바르지 않은 accessToken 입니다.", returnedServerResponse);
      }
    }

    @Nested
    @DisplayName("When: accessToken 없이 방 생성을 요청하면,")
    class When_accessToken_없이_방_생성을_요청하면 {

      ResponseSpec responseSpec = webTestClient
          .post()
          .uri("/rank/lobby/rankroom")
          .exchange();

      @Test
      @DisplayName("에러코드 400을 메세지와 함께 반환한다.")
      void 에러코드_400을_메세지와_함께_반환한다() {

        String returnedServerResponse = responseSpec
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .returnResult()
            .getResponseBody();

        assertEquals("요청에 쿠키가 존재하지 않습니다.", returnedServerResponse);
      }
    }
  }
}
