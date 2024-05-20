package org.goorm.wordsketch.rank.rankroom;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankGameService {

  private final TaskScheduler taskScheduler;
  private final RankRoomService rankRoomService;
  private final SimpMessagingTemplate simpMessagingTemplate;
  private Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

  @Async
  public void startRankGame(String userUUID) {

    String roomUUID = rankRoomService.startRankGame(userUUID);
    scheduledFutures.put("GUESS-" + roomUUID, taskScheduler.schedule(() -> guessStep(roomUUID),
        Instant.now().plusSeconds(5)));

    simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.VIEW)
        .payload("COUNT-DOWN")
        .build());
  }

  @Async
  public void guessStep(String roomUUID) {

    String imageURL = rankRoomService.guessStep(roomUUID);
    scheduledFutures.put("NO_SCORE-" + roomUUID, taskScheduler.schedule(() -> noScoreStep(roomUUID),
        Instant.now().plusSeconds(30)));

    simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.VIEW)
        .payload("GUESS")
        .imageURL(imageURL)
        .build());
  }

  public void validateAnswer(String roomUUID, String userUUID, String payload) {

    RankRoomMessage rankRoomMessage = rankRoomService.validateAnswer(roomUUID, userUUID, payload);
    if (rankRoomMessage == null)
      return;

    // 최초 정답자라면, 실패 로직 예약을 취소
    ScheduledFuture<?> scheduledFuture = scheduledFutures.get("NO_SCORE-" + roomUUID);
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
      // scheduledFutures.remove("SCORE-" + roomUUID);

      scheduledFutures.put("CHECK-" + roomUUID, taskScheduler.schedule(() -> checkRound(roomUUID),
          Instant.now().plusSeconds(5)));
      // 성공 및 득점 메세지 전파
      simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, rankRoomMessage);
    }
  }

  @Async
  public void noScoreStep(String roomUUID) {

    rankRoomService.skipRound(roomUUID);
    scheduledFutures.put("CHECK-" + roomUUID, taskScheduler.schedule(() -> checkRound(roomUUID),
        Instant.now().plusSeconds(5)));

    // 아무도 정답을 맞추지 못했으므로, 실패 메세지 전파
    simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.VIEW)
        .payload("NO_SCORE")
        .build());
  }

  @Async
  public void checkRound(String roomUUID) {

    RankRoomMessage rankRoomMessage = rankRoomService.isLastRound(roomUUID);
    if (rankRoomMessage != null) {

      simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, rankRoomMessage);
      return;
    }

    scheduledFutures.put("GUESS-" + roomUUID, taskScheduler.schedule(() -> guessStep(roomUUID),
        Instant.now().plusSeconds(5)));

    simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.VIEW)
        .payload("COUNT-DOWN")
        .build());
  }
}
