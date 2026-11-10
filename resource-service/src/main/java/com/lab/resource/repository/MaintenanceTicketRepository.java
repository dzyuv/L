package com.lab.resource.repository;
import com.lab.resource.entity.MaintenanceTicket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Collection;

public interface MaintenanceTicketRepository extends CrudMapper<MaintenanceTicket> {
    default List<MaintenanceTicket> findByReportedByOrderByCreatedAtDesc(Long reportedBy) {
        return selectList(Wrappers.<MaintenanceTicket>query().eq("reported_by", reportedBy).orderByDesc("created_at"));
    }
    default IPage<MaintenanceTicket> search(String status, Long assetId, Page<MaintenanceTicket> page) {
        QueryWrapper<MaintenanceTicket> wrapper = new QueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq("status", status);
        if (assetId != null) wrapper.eq("asset_id", assetId);
        wrapper.orderByDesc("created_at");
        return selectPage(page, wrapper);
    }
    default boolean existsByAssetIdAndStatusIn(Long assetId, Collection<String> statuses) {
        return !statuses.isEmpty() && selectCount(Wrappers.<MaintenanceTicket>query()
                .eq("asset_id", assetId).in("status", statuses)) > 0;
    }
}
