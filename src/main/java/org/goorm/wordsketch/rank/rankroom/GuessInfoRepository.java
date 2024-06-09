package org.goorm.wordsketch.rank.rankroom;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;

public interface GuessInfoRepository extends CrudRepository<GameProgressInfo, String> {

  @NonNull
  Optional<GameProgressInfo> findById(@NonNull String roomUUID);
}
