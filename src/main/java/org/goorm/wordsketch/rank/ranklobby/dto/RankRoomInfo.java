package org.goorm.wordsketch.rank.ranklobby.dto;

import org.goorm.wordsketch.rank.ranklobby.RankRoom;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankRoomInfo {

  private String roomUUID;
  private String roomName;
  private int headCount;

  public RankRoomInfo(RankRoom rankRoom) {

    this.roomUUID = rankRoom.getRoomUUID();
    this.roomName = rankRoom.getRoomName();
    this.headCount = rankRoom.getPlayers().size() - 1;
  }

  @Override
  public boolean equals(Object object) {

    RankRoomInfo rankRoom = (RankRoomInfo) object;
    return roomUUID.equals(rankRoom.roomUUID);
  }
}
