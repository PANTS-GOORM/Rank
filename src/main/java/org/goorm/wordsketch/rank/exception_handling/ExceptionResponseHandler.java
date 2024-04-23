package org.goorm.wordsketch.rank.exception_handling;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ExceptionResponseHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<String> handleResponseStatusException(ResponseStatusException responseStatusException) {

    return ResponseEntity
        .status(responseStatusException.getStatusCode())
        .body(responseStatusException.getBody().getDetail());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<String> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException httpMessageNotReadableException) {
    return ResponseEntity
        .status(400)
        .body("요청에 필요한 값들을 찾을 수 없습니다.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception exception) {

    return ResponseEntity
        .status(500)
        .body("알 수 없는 오류가 발생했습니다.");
  }
}
