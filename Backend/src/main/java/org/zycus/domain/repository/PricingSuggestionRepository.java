package org.zycus.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zycus.domain.model.PricingSuggestion;
import org.zycus.domain.model.SuggestionStatus;
import org.zycus.domain.model.TriggerReason;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    List<PricingSuggestion> findByStatus(SuggestionStatus status);

    List<PricingSuggestion> findByProductId(String productId);

    List<PricingSuggestion> findByProductIdAndStatus(String productId, SuggestionStatus status);

    boolean existsByProductIdAndTriggerReasonAndStatus(String productId, TriggerReason triggerReason, SuggestionStatus status);

    Optional<PricingSuggestion> findTopByProductIdOrderByCreatedAtDesc(String productId);

    @Query("SELECT ps FROM PricingSuggestion ps WHERE (:productId IS NULL OR ps.product.id = :productId) " +
           "AND (:status IS NULL OR ps.status = :status) " +
           "AND (:triggerReason IS NULL OR ps.triggerReason = :triggerReason) " +
           "ORDER BY ps.createdAt DESC")
    List<PricingSuggestion> search(
            @Param("productId") String productId,
            @Param("status") SuggestionStatus status,
            @Param("triggerReason") TriggerReason triggerReason
    );
}
