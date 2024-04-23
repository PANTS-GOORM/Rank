package org.goorm.wordsketch.rank.acceptance.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import org.goorm.wordsketch.rank.acceptance.global.AcceptanceTest;
import org.goorm.wordsketch.rank.ranklobby.RankLobbyService;
import org.goorm.wordsketch.rank.ranklobby.RankRoom;
import org.goorm.wordsketch.rank.ranklobby.dto.RankRoomInfo;
import org.goorm.wordsketch.rank.security.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import lombok.RequiredArgsConstructor;

@AcceptanceTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@DisplayName("경쟁 어휘 학습 로비 API 인수테스트")
public class RankRoomCheckApiTest {

  private final WebTestClient webTestClient;

  private final RankLobbyService rankLobbyService;

  @Value("${jwt.access.cookie}")
  private String accessCookie;

  @Autowired
  private JwtService jwtService;

  @Nested
  @DisplayName("Given: 서버에 방이 1개 있고,")
  class Given_서버에_방이_1개_있고 {

    RankRoom rankRoom = RankRoom.builder()
        .roomUUID(UUID.randomUUID().toString())
        .roomName("테스트 RankRoom")
        .headCount(1)
        .build();

    RankRoom savedRankRoom = rankLobbyService.registRankRoom(rankRoom);

    @Nested
    @DisplayName("When: 올바른 accessToken과 함께 방목록을 요청하면,")
    class When_올바른_accessToken과_함께_방목록을_요청하면 {

      private String accessToken = jwtService.createAccessTokenForTest("test@email.com");

      ResponseSpec responseSpec = webTestClient
          .get()
          .uri("/rank/lobby")
          .cookie(accessCookie, accessToken)
          .exchange();

      @Test
      @DisplayName("Then: 방 정보 객체를 반환한다.")
      void Then_방_정보_객체를_반환한다() {

        List<RankRoomInfo> returnedRoomListInfos = responseSpec
            .expectStatus().isOk()
            .expectBodyList(RankRoomInfo.class)
            .returnResult()
            .getResponseBody();

        // 사전에 생성한 방 정보를 배열에 담아서 비교
        List<RankRoomInfo> savedRankRoomInfos = new ArrayList<>();
        savedRankRoomInfos.add(new RankRoomInfo(savedRankRoom));
        assertEquals(savedRankRoomInfos, returnedRoomListInfos);
      }
    }

    @Nested
    @DisplayName("When: 올바르지 않은 accessToken과 함께 방목록을 요청하면,")
    class When_올바르지_않은_accessToken과_함께_방목록을_요청하면 {

      private String accessToken = "InvalidAccessToken";

      ResponseSpec responseSpec = webTestClient
          .get()
          .uri("/rank/lobby")
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
    @DisplayName("When: accessToken 없이 방목록을 요청하면,")
    class When_accessToken_없이_방목록을_요청하면 {

      ResponseSpec responseSpec = webTestClient
          .get()
          .uri("/rank/lobby")
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