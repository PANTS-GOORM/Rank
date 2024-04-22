package org.goorm.wordsketch.rank.ranklobby;

import java.util.List;

import org.goorm.wordsketch.rank.ranklobby.dto.RankRoomInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rank/lobby")
@RequiredArgsConstructor
public class RankLobbyController {

  private final RankLobbyService rankLobbyService;

  @GetMapping()
  public ResponseEntity<List<RankRoomInfo>> getAllRankRoomInfos(HttpServletRequest request) {

    List<RankRoomInfo> rankRoomInfos = rankLobbyService.getAllRankRooms()
        .stream()
        .map(RankRoomInfo::new)
        .toList();

    return ResponseEntity.ok(rankRoomInfos);
  }
}
