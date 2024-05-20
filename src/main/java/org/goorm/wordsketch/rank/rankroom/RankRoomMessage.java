package org.goorm.wordsketch.rank.rankroom;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankRoomMessage {

  private RankRoomMessageType rankRoomMessageType;
  private List<String> players;
  private List<Boolean> playersStatus;
  private int scorePlayerIdx;
  private int[] playersScore;
  private String payload;
  private String imageURL;
}
