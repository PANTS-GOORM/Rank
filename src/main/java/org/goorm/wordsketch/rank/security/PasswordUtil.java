package org.goorm.wordsketch.rank.security;

import java.util.Random;

public class PasswordUtil {

  /**
   * 소셜 로그인 유저를 Authenication에 등록하기 위한 임의의 비밀번호를 생성하는 함수
   * 
   * @return : 영문 대문자, 소문자, 숫자를 랜덤하게 조합한 8자리 비밀번호 문자열
   */
  public static String generateRandomPassword() {

    int index = 0;
    char[] charSet = new char[] {

        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    }; // 배열안의 문자 숫자는 원하는대로

    StringBuffer password = new StringBuffer();
    Random random = new Random();

    for (int i = 0; i < 8; i++) {

      double rd = random.nextDouble();
      index = (int) (charSet.length * rd);

      password.append(charSet[index]);
    }

    // StringBuffer를 String으로 변환해서 return
    return password.toString();
  }
}