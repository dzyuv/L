package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    List<MaintenanceTicket> findByReportedByOrderByCreatedAtDesc(Long reportedBy);
    List<MaintenanceTicket> findAllByOrderByCreatedAtDesc();
    List<MaintenanceTicket> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByAssetIdAndStatusIn(Long assetId, Collection<String> statuses);
}
