package org.zycus.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zycus.domain.model.ReorderSuggestion;
import org.zycus.domain.model.SuggestionStatus;
import org.zycus.domain.model.TriggerReason;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    List<ReorderSuggestion> findByStatus(SuggestionStatus status);

    List<ReorderSuggestion> findByProductId(String productId);

    List<ReorderSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);

    boolean existsByProductIdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);

    Optional<ReorderSuggestion> findTopByProductIdOrderByCreatedAtDesc(String productId);

    @Query("SELECT rs FROM ReorderSuggestion rs WHERE (:productId IS NULL OR rs.product.id = :productId) " +
           "AND (:status IS NULL OR rs.status = :status) " +
           "AND (:triggerReason IS NULL OR rs.triggerReason = :triggerReason) " +
           "ORDER BY rs.createdAt DESC")
    List<ReorderSuggestion> search(
            @Param("productId") String productId,
            @Param("status") SuggestionStatus status,
            @Param("triggerReason") TriggerReason triggerReason
    );
}
