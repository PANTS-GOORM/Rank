package org.goorm.wordsketch.rank.rankroom;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rankroom")
public class RankRoomController {

  private final RankRoomService rankRoomService;
  private final RankGameService rankGameService;
  private final SimpMessagingTemplate simpMessagingTemplate;

  @SubscribeMapping("/rankroom/{roomUUID}")
  @SendTo("/sub/rankroom/{roomUUID}")
  public RankRoomMessage initialReply(@DestinationVariable String roomUUID, Principal principal) {

    return rankRoomService.getInitialReplyData(roomUUID, principal.getName());
  }

  @MessageMapping("/rankroom/{roomUUID}")
  @SendTo("/sub/rankroom/{roomUUID}")
  public RankRoomMessage sendChat(@DestinationVariable String roomUUID, String payload, Principal principal)
      throws Exception {

    rankGameService.validateAnswer(roomUUID, principal.getName(), payload);

    return RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.CHAT)
        .payload(principal.getName() + ":" + payload)
        .build();
  }

  @PutMapping("/player-status")
  public ResponseEntity<String> switchPlayerStatus(Principal principal) {

    String roomUUID = rankRoomService.getUserLocation(principal.getName()).getRoomUUID();
    List<Boolean> playersStatus = rankRoomService.setPlayerStatus(principal.getName(), roomUUID);

    // 게임방에 참가자 준비 상태 전파
    simpMessagingTemplate.convertAndSend("/sub/rankroom/" + roomUUID, RankRoomMessage.builder()
        .rankRoomMessageType(RankRoomMessageType.STATUS)
        .playersStatus(playersStatus)
        .build());

    return ResponseEntity.ok("성공적으로 상태를 변경했습니다.");
  }

  @PostMapping("rankgame")
  public ResponseEntity<Void> startRankGame(Principal principal) {

    rankGameService.startRankGame(principal.getName());

    return ResponseEntity.ok().build();
  }
}
