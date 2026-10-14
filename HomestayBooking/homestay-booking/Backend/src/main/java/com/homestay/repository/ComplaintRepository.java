package com.homestay.repository;

import com.homestay.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    @Query("SELECT c FROM Complaint c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR (c.user IS NOT NULL AND LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))))")
    Page<Complaint> findByFilters(
            @Param("status") Complaint.Status status,
            @Param("search") String search,
            Pageable pageable
    );

    // SCR-54: EntityGraph thay JOIN FETCH để pagination đúng trên DB.
    @EntityGraph(attributePaths = {"user"})
    @Query(value = """
            SELECT c FROM Complaint c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   (c.user IS NOT NULL AND (
                     LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   )))
            ORDER BY c.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Complaint c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:search IS NULL OR :search = '' OR
                   LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   (c.user IS NOT NULL AND (
                     LOWER(c.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
                     LOWER(c.user.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   )))
            """)
    Page<Complaint> findForAdmin(
            @Param("status") Complaint.Status status,
            @Param("search") String search,
            Pageable pageable);

    /** SCR-54 — Load complaint + user for resolve/status update mapping. */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT c FROM Complaint c WHERE c.id = :id")
    Optional<Complaint> findByIdWithUser(@Param("id") UUID id);

    Page<Complaint> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Complaint> findByIdAndUserId(UUID id, UUID userId);
}
