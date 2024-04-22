package org.goorm.wordsketch.rank.ranklobby;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankLobbyService {

  private final RankRoomRepository rankRoomRepository;

  /**
   * 인자로 받은 RankRoomInfo를 DB에 저장하는 함수
   * 
   * @param rankRoomInfo 생성하려는 RankRoom의 정보를 담은 RankRoomInfo
   */
  public RankRoom registRankRoom(RankRoom rankRoom) {

    return rankRoomRepository.save(rankRoom);
  }

  /**
   * 현재 생성된 모든 RankRoom들의 RankRoomInfo를 담은 리스트를 반환하는 함수
   * 
   * @return : 리스트 타입의 생성된 모든 방의 정보들
   */
  public List<RankRoom> getAllRankRooms() {

    return rankRoomRepository.findAll();
  }

  /**
   * 주어진 방 이름과 일치하는 RankRoom Entity를 반환하는 함수
   * 
   * @param roomName
   * @return
   */
  public RankRoom getRankRoom(String roomName) {

    return null;
  }
}
