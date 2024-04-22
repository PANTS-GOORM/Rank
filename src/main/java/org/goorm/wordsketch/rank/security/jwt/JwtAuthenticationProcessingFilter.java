package org.goorm.wordsketch.rank.security.jwt;

import lombok.RequiredArgsConstructor;

import org.goorm.wordsketch.rank.security.PasswordUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * <pre>
* JWT 인증 필터
*
* 기본적으로 사용자는 요청에 accessToken을 Cookie에 담아서 요청
*
* 1. accessToken이 유효한 경우 -> 인증 성공 처리
* 2. accessToken이 없는 경우 -> 400(BAD_REQUEST) 에러와 함께 인증 실패 처리
* 3. accessToken이 유효하지 않을 경우 -> 401(UNAUTHORIZED) 에러와 함께 인증 실패 처리
 * </pre>
 */
@RequiredArgsConstructor
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

  @SuppressWarnings("null")
  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // accessToken을 검사하고 인증을 처리하는 함수 호출
    // accessToken이 유효하다면, 인증 객체가 담긴 상태로 다음 필터로 넘어가기 때문에 인증 성공
    // 인증 객체가 담기지 않은 상태로 다음 필터로 넘어간다면 403(FORBIDDEN) 에러 발생
    checkAccessTokenAndAuthentication(request, response, filterChain);

    // 예외가 발생했다면 request에 예외를 담은채로 다음 필터 진행
    filterChain.doFilter(request, response);
  }

  /**
   * <pre>
  * accessToken 체크 및 인증 처리 메소드
  *
  * request에서 extractAccessToken()으로 Access Token 추출 후,
  * JWTService::isTokenValid()로 유효한 토큰인지 검증
  *
  * 유효한 토큰이면, 액세스 토큰에서 extractEmail로 유저 Email을 추출한 후
  * UserService::findByEmail()로 해당 이메일을 사용하는 유저 객체 반환
  *
  * 반환받은 유저 객체를 saveAuthentication()으로 인증 처리하여
  * 인증 허가 처리된 객체를 SecurityContextHolder에 담기
  * 그 후 다음 인증 필터로 진행
   * </pre>
   *
   * @param request
   * @param response
   * @param filterChain
   * @throws ServletException
   * @throws IOException
   */
  public void checkAccessTokenAndAuthentication(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    jwtService.extractAccessToken(request)
        .flatMap(accessToken -> jwtService.extractUserEmail(request, accessToken))
        .ifPresent(this::saveAuthentication);
  }

  /**
   * <pre>
  * 인증 허가 메소드
  *
  * new UsernamePasswordAuthenticationToken()로 인증 객체인 Authentication 객체 생성
  *
  * UsernamePasswordAuthenticationToken의 파라미터
  * 1. 위에서 만든 UserDetailsUser 객체 (유저 정보)
  * 2. credential(보통 비밀번호로, 인증 시에는 보통 null로 제거)
  *
  * SecurityContextHolder.getContext()로 SecurityContext를 꺼낸 후,
  * setAuthentication()을 이용하여 위에서 만든 Authentication 객체에 대한 인증 허가 처리
   * </pre>
   *
   * @param userEmail String 형식의 유저의 Email
   */
  public void saveAuthentication(String userEmail) {

    // 소셜 로그인 유저는 따로 비밀번호가 없으므로 임의의 비밀번호 생성
    String password = PasswordUtil.generateRandomPassword();

    UserDetails userDetails = User.builder()
        .username(userEmail)
        .password(password)
        .roles("USER")
        .build();

    // authorities가 포함되지 않으면 Spring Security가 403 반환
    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        authoritiesMapper.mapAuthorities(userDetails.getAuthorities()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
