package org.goorm.wordsketch.rank.ranklobby;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@RedisHash
public class RankRoom {

  @Id
  private String roomUUID;

  private String roomName;
  private int headCount;

  @Builder.Default
  private LocalDateTime createdDate = LocalDateTime.now();
}
