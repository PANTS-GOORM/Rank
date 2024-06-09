package org.goorm.wordsketch.rank.ranklobby;

import java.util.Optional;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.lang.NonNull;

public interface RankRoomRepository extends ListCrudRepository<RankRoom, String> {

  @NonNull
  Optional<RankRoom> findById(@NonNull String roomUUID);

  Optional<RankRoom> findByRoomName(String roomName);
}
