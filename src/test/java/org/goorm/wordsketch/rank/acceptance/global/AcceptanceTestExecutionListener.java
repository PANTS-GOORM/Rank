package org.goorm.wordsketch.rank.acceptance.global;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test의 일관성을 위해, 매 Test마다 Redis를 초기화 하는 클래스
 */
@SuppressWarnings({ "null", "unchecked" })
public class AcceptanceTestExecutionListener extends AbstractTestExecutionListener {

  @Override
  public void beforeTestClass(final TestContext testContext) {

    final RedisTemplate<String, Object> redisTemplate = testContext.getApplicationContext()
        .getBean(RedisTemplate.class);
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  @Override
  public void afterTestMethod(final TestContext testContext) {

    final RedisTemplate<String, Object> redisTemplate = testContext.getApplicationContext()
        .getBean(RedisTemplate.class);
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }
}
