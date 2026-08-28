package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Collection;

public interface MaintenanceTicketRepository extends CrudMapper<MaintenanceTicket> {
    default List<MaintenanceTicket> findByReportedByOrderByCreatedAtDesc(Long reportedBy) {
        return selectList(Wrappers.<MaintenanceTicket>query().eq("reported_by", reportedBy).orderByDesc("created_at"));
    }
    default List<MaintenanceTicket> findAllByOrderByCreatedAtDesc() {
        return selectList(Wrappers.<MaintenanceTicket>query().orderByDesc("created_at"));
    }
    default List<MaintenanceTicket> findByStatusOrderByCreatedAtDesc(String status) {
        return selectList(Wrappers.<MaintenanceTicket>query().eq("status", status).orderByDesc("created_at"));
    }
    default boolean existsByAssetIdAndStatusIn(Long assetId, Collection<String> statuses) {
        return !statuses.isEmpty() && selectCount(Wrappers.<MaintenanceTicket>query()
                .eq("asset_id", assetId).in("status", statuses)) > 0;
    }
}
