package org.goorm.wordsketch.rank.ranklobby;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;

public interface RankRoomRepository extends ListCrudRepository<RankRoom, String> {

  Optional<RankRoom> findByRoomName(String roomName);
}
