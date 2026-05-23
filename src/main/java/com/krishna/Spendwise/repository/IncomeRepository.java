package com.krishna.Spendwise.repository;


import com.krishna.Spendwise.domain.entity.IncomeEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncomeRepository extends JpaRepository<IncomeEntity, Long> {
    List<IncomeEntity> findByProfileIdOrderByDateDesc(Long profileId);

    List<IncomeEntity> findTop5ByProfileIdOrderByDateDesc(Long profileId);

    @Query("SELECT SUM(i.amount) FROM IncomeEntity i WHERE i.profile.id = :profileId")
    BigDecimal findTotalIncomeByProfileId(@Param("profileId") Long profileId);

    List<IncomeEntity> findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
            Long profileId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Sort sort
    );

    List<IncomeEntity> findByProfileIdAndDateBetween(Long profileId, LocalDate startDate, LocalDate endDate);

    Optional<IncomeEntity> findByIdAndProfileId(Long id, Long profileId);

    // For reports: sum income grouped by category name within a date range
    @Query("SELECT i.category.name, SUM(i.amount) FROM IncomeEntity i " +
           "WHERE i.profile.id = :profileId AND i.date BETWEEN :startDate AND :endDate " +
           "GROUP BY i.category.name")
    List<Object[]> sumIncomeGroupedByCategoryBetween(
            @Param("profileId") Long profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // For goals: total income since a given start date
    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeEntity i " +
           "WHERE i.profile.id = :profileId AND i.date >= :startDate")
    BigDecimal sumIncomeFromDate(@Param("profileId") Long profileId, @Param("startDate") LocalDate startDate);
}
