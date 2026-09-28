package org.zycus.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zycus.domain.model.InventorySnapshot;

import java.util.List;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {
    List<InventorySnapshot> findByProductIdOrderByTimestampDesc(String productId);
}
