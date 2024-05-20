package org.goorm.wordsketch.rank.rankroom;

import java.util.List;
import java.util.Optional;

import org.goorm.wordsketch.rank.ranklobby.RankRoom;
import org.goorm.wordsketch.rank.ranklobby.RankRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankRoomService {

  private final RankRoomRepository rankRoomRepository;
  private final GuessInfoRepository gameProgressInfoRepository;
  private final UserLocationRepository userLocationRepository;

  private final String GUESS_PREFIX = "GUESS";

  /**
   * roomUUID에 해당하는 RankRoom을 반환하는 함수
   * 대응하는 RankRoom이 존재하지 않으면 404 에러 반환
   * 
   * @param roomUUID
   * @return
   */
  public RankRoom getRankRoomByRoomUUID(String roomUUID) {

    return rankRoomRepository.findById(roomUUID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당하는 경쟁방이 존재하지 않습니다."));
  }

  /**
   * roomUUID에 해당하는 GuessInfo를 반환하는 함수
   * 대응하는 GuessInfo이 존재하지 않으면 404 에러 반환
   * 
   * @param roomUUID
   * @return
   */
  public GameProgressInfo getGuessInfoByRoomUUID(String roomUUID) {

    return gameProgressInfoRepository.findById(GUESS_PREFIX + roomUUID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당하는 경쟁방이 존재하지 않습니다."));
  }

  public UserLocation getUserLocation(String userUUID) {

    return userLocationRepository.findById(userUUID)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저의 위치를 확인할 수 없습니다."));
  }

  /**
   * roomUUID에 해당하는 RankRoom의 인원 수를 1 증가시키고, 참여자 정보에 userUUID을 추가하는 함수
   * 
   * @param roomUUID
   * @param userUUID
   */
  public void rankRoomEntrance(String roomUUID, String userUUID) {

    // 유저가 참가 중인 방 정보 받아오기
    Optional<UserLocation> prevUserLocation = userLocationRepository.findById(userUUID);

    // 유저가 방에 존재한다면 기존 RankRoom에 퇴장 처리
    if (prevUserLocation.isPresent())
      rankRoomExit(userUUID);

    // roomUUID에 해당하는 RankRoom 조회
    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);

    // RankRoom players 리스트에 추가
    rankRoom.getPlayers().add(userUUID);
    rankRoom.getPlayersStatus().add(false);

    // 유저가 참가 중인 방 정보 업데이트
    UserLocation userLocation = UserLocation.builder()
        .userUUID(userUUID)
        .roomUUID(roomUUID)
        .build();

    userLocationRepository.save(userLocation);

    // 변경한 RankRoom 정보를 DB에 영속화
    rankRoomRepository.save(rankRoom);
  }

  /**
   * roomUUID에 해당하는 RankRoom의 인원 수를 1 감소시키고, 참여자 정보에 userUUID을 제거하는 함수
   * 
   * @param roomUUID
   * @param userUUID
   */
  public void rankRoomExit(String userUUID) {

    // 유저가 참가 중인 방 정보 받아오기
    Optional<UserLocation> userLocation = userLocationRepository.findById(userUUID);

    // 유저가 방에 존재하지 않는다면 종료
    if (!userLocation.isPresent())
      return;

    UserLocation userLocationInfo = userLocation.get();

    // 유저가 방에 존재한다면 roomUUID에 해당하는 RankRoom에 퇴장정보 업데이트
    String roomUUID = userLocationInfo.getRoomUUID();

    // roomUUID에 해당하는 RankRoom이 존재하지 않으면 404 에러 반환
    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);

    // RankRoom players와 status 리스트에서 제거
    int playerIdx = rankRoom.getPlayers().indexOf(userUUID);
    rankRoom.getPlayers().remove(userUUID);
    rankRoom.getPlayersStatus().remove(playerIdx);

    // 변경한 RankRoom 정보를 DB에 영속화
    rankRoomRepository.save(rankRoom);

    // 유저가 참가 중인 방 정보 삭제
    userLocationRepository.delete(userLocationInfo);
  }

  public RankRoomMessage getInitialReplyData(String roomUUID, String userUUID) {

    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);
    List<String> players = rankRoom.getPlayers();
    List<Boolean> playersStatus = rankRoom.getPlayersStatus();

    return RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.ENTER)
        .players(players)
        .playersStatus(playersStatus)
        .payload(userUUID + "님이 입장하셨습니다.")
        .build();
  }

  /**
   * 유저가 속한 게임방에 접근하여 유저의 준비 상태를 변경하는 함수
   * 
   * @param userUUID
   * @param ready
   * @return 게임방에 속한 모든 참가자의 준비 상태 배열
   */
  public List<Boolean> setPlayerStatus(String userUUID, String roomUUID) {

    // 유저가 속해있는 게임방에 준비 상태 업데이트
    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);
    int idxOfPlayer = rankRoom.getPlayers().indexOf(userUUID);
    rankRoom.getPlayersStatus().set(idxOfPlayer, !rankRoom.getPlayersStatus().get(idxOfPlayer));
    rankRoomRepository.save(rankRoom);

    // 게임방에 속한 모든 참가자의 준비 상태 배열 반환
    return rankRoom.getPlayersStatus();
  }

  /**
   * RankRoom의 준비상태를 검증 후, 카운트다운 단계로 진입
   * 
   * @param roomUUID
   */
  public String startRankGame(String userUUID) {

    String roomUUID = getUserLocation(userUUID).getRoomUUID();
    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);
    List<Boolean> playersStatus = rankRoom.getPlayersStatus();
    for (boolean playerStatus : playersStatus) {

      if (playerStatus == false)
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "준비되지 않은 참가자가 있습니다.");
    }

    rankRoom.setInGame(true);
    rankRoomRepository.save(rankRoom);

    // TODO 라운드 설정 받아오기
    int wholeRound = 5;

    // 게임 진행 정보 생성
    gameProgressInfoRepository.save(GameProgressInfo.builder()
        .roomUUID(GUESS_PREFIX + roomUUID)
        .wholeRound(wholeRound)
        .rankRoomStatus(RankRoomStatus.COUNTDOWN)
        .players(rankRoom.getPlayers())
        .scores(new int[rankRoom.getPlayers().size()])
        .build());

    return roomUUID;
  }

  /**
   * 출제할 문제를 선택해서 게임 진행 상황을 업데이트하는 함수
   * 
   * @param roomUUID
   * @return 어휘와 관련된 이미지가 업로드된 URL
   */
  public String guessStep(String roomUUID) {

    // TODO 문제 선택 로직으로 수정
    String answer = "필름";
    String imageURL = "https://jogakbo-album-image-storage.s3.ap-northeast-2.amazonaws.com/05421507-ac7f-4e85-9b8a-69a1b55ce5c1/7d7e0e08-3292-4407-9620-706fa9f8eaa0.png";

    GameProgressInfo gameProgressInfo = getGuessInfoByRoomUUID(roomUUID);
    gameProgressInfo.setAnswer(answer);
    gameProgressInfo.setImageURL(imageURL);
    gameProgressInfo.setRankRoomStatus(RankRoomStatus.GUESS);
    gameProgressInfoRepository.save(gameProgressInfo);

    return imageURL;
  }

  public RankRoomMessage validateAnswer(String roomUUID, String userUUID, String paylaod) {

    if (!getRankRoomByRoomUUID(roomUUID).isInGame())
      return null;

    GameProgressInfo gameProgressInfo = getGuessInfoByRoomUUID(roomUUID);
    if (gameProgressInfo.getRankRoomStatus() != RankRoomStatus.GUESS || !gameProgressInfo.getAnswer().equals(paylaod))
      return null;

    gameProgressInfo.setRound(gameProgressInfo.getRound() + 1);
    gameProgressInfo.setRankRoomStatus(RankRoomStatus.SCORE);
    int playerIdx = gameProgressInfo.getPlayers().indexOf(userUUID);
    gameProgressInfo.getScores()[playerIdx]++;
    gameProgressInfoRepository.save(gameProgressInfo);

    return RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.VIEW)
        .payload("SCORE")
        .players(gameProgressInfo.getPlayers())
        .scorePlayerIdx(playerIdx)
        .playersScore(gameProgressInfo.getScores())
        .build();
  }

  public void skipRound(String roomUUID) {

    GameProgressInfo gameProgressInfo = getGuessInfoByRoomUUID(roomUUID);
    gameProgressInfo.setRound(gameProgressInfo.getRound() + 1);
    gameProgressInfoRepository.save(gameProgressInfo);
  }

  public RankRoomMessage isLastRound(String roomUUID) {

    GameProgressInfo gameProgressInfo = getGuessInfoByRoomUUID(roomUUID);

    if (gameProgressInfo.getRound() == gameProgressInfo.getWholeRound()) {

      endGame(roomUUID);

      return RankRoomMessage.builder()
          .rankRoomMessageType(RankRoomMessageType.VIEW)
          .payload("RESULT")
          .players(gameProgressInfo.getPlayers())
          .playersScore(gameProgressInfo.getScores())
          .build();
    }

    return null;
  }

  public void endGame(String roomUUID) {

    RankRoom rankRoom = getRankRoomByRoomUUID(roomUUID);

    // 플레이어 대기 상태 초기화
    rankRoom.setPlayersStatus(
        rankRoom.getPlayersStatus().stream().map((status) -> true).toList());

    // 게임방 상태 변경
    rankRoom.setInGame(false);
    rankRoomRepository.save(rankRoom);

    // 게임 진행 정보 삭제
    GameProgressInfo gameProgressInfo = getGuessInfoByRoomUUID(roomUUID);
    gameProgressInfoRepository.delete(gameProgressInfo);
  }
}