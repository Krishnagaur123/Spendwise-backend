package com.krishna.Spendwise.repository;

import com.krishna.Spendwise.domain.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity,Long> {

    List<CategoryEntity> findByProfileId(Long ProfileId);

    Optional<CategoryEntity> findByIdAndProfileId(Long Id, Long ProfileId);

    List<CategoryEntity> findByTypeAndProfileId(String type, Long ProfileId);

    Boolean existsByNameAndProfileId(String Name, Long ProfileId);

    Optional<CategoryEntity> findByNameAndProfileId(String name, Long profileId);

    List<CategoryEntity> findByIsDefaultTrue();

    // Check if category is referenced by any income or expense
    @Query("SELECT COUNT(i) FROM IncomeEntity i WHERE i.category.id = :categoryId")
    long countIncomesByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(e) FROM ExpenseEntity e WHERE e.category.id = :categoryId")
    long countExpensesByCategory(@Param("categoryId") Long categoryId);
}
