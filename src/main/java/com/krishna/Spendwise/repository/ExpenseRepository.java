package com.krishna.Spendwise.repository;

import com.krishna.Spendwise.domain.entity.ExpenseEntity;
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
public interface ExpenseRepository extends JpaRepository<ExpenseEntity, Long> {

    List<ExpenseEntity> findByProfileIdOrderByDateDesc(Long profileId);

    List<ExpenseEntity> findTop5ByProfileIdOrderByDateDesc(Long profileId);

    @Query("SELECT SUM(e.amount) FROM ExpenseEntity e WHERE e.profile.id = :profileId")
    BigDecimal findTotalExpenseByProfileId(@Param("profileId") Long profileId);

    List<ExpenseEntity> findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(
            Long profileId,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Sort sort
    );

    List<ExpenseEntity> findByProfileIdAndDateBetween(Long profileId, LocalDate startDate, LocalDate endDate);

    List<ExpenseEntity> findByProfileIdAndDate(Long profileId, LocalDate date);

    Optional<ExpenseEntity> findByIdAndProfileId(Long id, Long profileId);

    // For reports: sum expense grouped by category name within a date range
    @Query("SELECT e.category.name, SUM(e.amount) FROM ExpenseEntity e " +
           "WHERE e.profile.id = :profileId AND e.date BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category.name")
    List<Object[]> sumExpenseGroupedByCategoryBetween(
            @Param("profileId") Long profileId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // For goals: total expense since a given start date
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM ExpenseEntity e " +
           "WHERE e.profile.id = :profileId AND e.date >= :startDate")
    BigDecimal sumExpenseFromDate(@Param("profileId") Long profileId, @Param("startDate") LocalDate startDate);
}
