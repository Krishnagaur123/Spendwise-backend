package com.krishna.Spendwise.repository;

import com.krishna.Spendwise.domain.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, Long> {

    List<GoalEntity> findByProfileId(Long profileId);

    Optional<GoalEntity> findByIdAndProfileId(Long id, Long profileId);
}
