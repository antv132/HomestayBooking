package com.homestay.repository;

import com.homestay.entity.InspectionChecklistAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InspectionChecklistAnswerRepository extends JpaRepository<InspectionChecklistAnswer, UUID> {

    List<InspectionChecklistAnswer> findByInspectionIdOrderByChecklistItem_SortOrderAsc(UUID inspectionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InspectionChecklistAnswer a WHERE a.inspection.id = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") UUID inspectionId);
}
