package org.goorm.wordsketch.rank.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
@Getter
public class JwtService {

  @Value("${jwt.secretKey}")
  private String secretKey;

  @Value("${jwt.access.cookie}")
  private String accessCookie;

  private final String ISSUER = "WORD SKETCH";
  private final String ACCESS_TOKEN_SUBJECT = "AccessToken";
  private final String USERNAME_CLAIM = "username";
  private final String AUTHRITIES_CLAIM = "authorities";

  /**
   * <pre>
  * 쿠키 내에서 accessToken을 추출하는 함수
  *
  * accessToken이 존재하지 않는다면 ResponseStatusException(HttpStatus.BAD_REQUEST) 예외
  반환
   * </pre>
   *
   * @param request 쿠키에서 accessToken을 추출하기 위해 사용
   * @return 문자열 타입의 accessToken
   * @throws ResponseStatusException 쿠기 또는 accessToken 이 존재하지 않는다면
   *                                 HttpStatus.BAD_REQUEST 상태로 반환
   */
  public Optional<String> extractAccessToken(HttpServletRequest request) {

    // 요청에서 쿠키 추출
    // 존재하지 않는다면 400(BAD_REQUEST) 반환
    Optional<Cookie[]> cookies = Optional.ofNullable(request.getCookies());

    if (cookies.isEmpty()) {

      request.setAttribute("FilterException", new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "요청에 쿠키가 존재하지 않습니다."));

      return Optional.empty();
    }

    // 쿠키에서 accessToken 추출
    // 존재하지 않는다면 400(BAD_REQUEST) 반환
    Optional<Cookie> accessTokenCookie = Arrays.stream(cookies.get())
        .filter(cookie -> cookie.getName().equals(accessCookie))
        .findFirst();

    if (accessTokenCookie.isEmpty()) {

      request.setAttribute("FilterException", new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "요청에 accessToken이 존재하지 않습니다."));

      return Optional.empty();
    }

    return Optional.ofNullable(accessTokenCookie
        .get()
        .getValue());
  }

  /**
   * accessToken에서 유저의 Email 추출하는 함수
   *
   * @param accessToken
   * @return JWT가 유효하다면 Email을 String 타입으로 반환
   * @throws ResponseStatusException JWT가 유효하지 않다면 HttpStatus.UNAUTHORIZED 상태로
   *                                 반환
   */
  public Optional<String> extractUserEmail(HttpServletRequest request, String accessToken) {

    try {

      Claims claims = Jwts.parser()
          .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
          .build()
          .parseSignedClaims(accessToken)
          .getPayload();

      return Optional.ofNullable(String.valueOf(claims.get(USERNAME_CLAIM)));

    } catch (JwtException jwtException) {

      ResponseStatusException responseStatusException = new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "올바르지 않은 accessToken 입니다.");
      request.setAttribute("FilterException", responseStatusException);

      return Optional.empty();

    } catch (NullPointerException | IllegalArgumentException complexException) {

      // TODO 서버가 사인한 JWT지만 유저 Email값이 존재하지 않을 경우, SNS로 알림 보내기?
      return Optional.empty();
    }
  }

  /**
   * AccessToken 생성, 응답 헤더에 추가
   *
   * @param response       응답
   * @param authentication 인증, 인가 정보
   */
  public String createAccessTokenForTest(String userName) {
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    return Jwts.builder().issuer(ISSUER).subject(ACCESS_TOKEN_SUBJECT)
        .claim(USERNAME_CLAIM, userName)
        .signWith(key).compact();
  }
}
