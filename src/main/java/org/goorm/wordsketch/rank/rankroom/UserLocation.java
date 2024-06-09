package org.goorm.wordsketch.rank.rankroom;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RedisHash
public class UserLocation {

  @Id
  private String userUUID;

  private String roomUUID;
}
