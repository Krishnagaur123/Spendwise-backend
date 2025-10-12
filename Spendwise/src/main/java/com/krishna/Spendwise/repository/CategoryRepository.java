package com.krishna.Spendwise.repository;

import com.krishna.Spendwise.domain.entity.CategoryEntity;
import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity,Long> {

    List<CategoryEntity> findByProfileId(Long ProfileId);

    Optional<CategoryEntity> findByIdAndProfileId(Long Id, Long ProfileId);

    List<CategoryEntity> findByTypeAndProfileId(String type,Long ProfileId);

    Boolean existsByNameAndProfileId(String Name,Long ProfileId);
}
