package com.krishna.Spendwise.repository;

import com.krishna.Spendwise.domain.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByProfileId(Long profileId);
    Optional<CategoryEntity> findByIdAndProfileId(Long id, Long profileId);
    List<CategoryEntity> findByTypeAndProfileId(String type, Long profileId);
    Boolean existsByNameAndProfileId(String name, Long profileId);
    Optional<CategoryEntity> findByNameAndProfileId(String name, Long profileId);

    /** Returns all system-default categories (shared across users). */
    List<CategoryEntity> findByIsDefaultTrue();

    /** Looks up a single default category by name — used when resolving transaction categories. */
    Optional<CategoryEntity> findByIsDefaultTrueAndName(String name);

    /** Used to prevent deletion of categories that are still referenced by transactions. */
    @Query("SELECT COUNT(i) FROM IncomeEntity i WHERE i.category.id = :categoryId")
    long countIncomesByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(e) FROM ExpenseEntity e WHERE e.category.id = :categoryId")
    long countExpensesByCategory(@Param("categoryId") Long categoryId);
}
