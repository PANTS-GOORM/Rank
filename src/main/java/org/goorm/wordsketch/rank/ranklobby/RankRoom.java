package org.goorm.wordsketch.rank.ranklobby;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RedisHash
public class RankRoom {

  @Id
  private String roomUUID;

  @Indexed
  private String roomName;

  @Builder.Default
  private boolean inGame = false;

  @Builder.Default
  private List<String> players = Arrays.asList("");

  @Builder.Default
  private List<Boolean> playersStatus = Arrays.asList(true);

  @Builder.Default
  private LocalDateTime createdDate = LocalDateTime.now();
}
