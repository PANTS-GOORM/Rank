package org.goorm.wordsketch.rank.rankroom;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RedisHash
public class GameProgressInfo {

  @Id
  private String roomUUID;

  @Builder.Default
  private int round = 0;

  private int wholeRound;

  private RankRoomStatus rankRoomStatus;

  private String answer;
  private String imageURL;

  private List<String> players;
  private int[] scores;
}
