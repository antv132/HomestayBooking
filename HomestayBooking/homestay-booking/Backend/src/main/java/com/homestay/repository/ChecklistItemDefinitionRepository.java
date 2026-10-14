package com.homestay.repository;

import com.homestay.entity.ChecklistItemDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistItemDefinitionRepository extends JpaRepository<ChecklistItemDefinition, UUID> {

    List<ChecklistItemDefinition> findByIsActiveTrueAndPropertyIsNullOrderBySortOrderAsc();

    Optional<ChecklistItemDefinition> findByCodeAndPropertyIsNull(String code);

    @Query("SELECT COUNT(c) FROM ChecklistItemDefinition c WHERE c.property IS NULL")
    long countGlobal();
}
