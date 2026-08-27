package com.lab.resource.service;

import com.lab.resource.*;
import com.lab.resource.controller.AssetController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public interface AssetService {
    List<AssetCategory> categories(boolean admin, HttpServletRequest request);
    AssetCategory createCategory(AssetController.CategoryRequest body, HttpServletRequest request);
    AssetCategory updateCategory(Long id, AssetController.CategoryRequest body, HttpServletRequest request);
    List<?> catalog(HttpServletRequest request);
    Map<String, Object> listAssets(String query, String status, Long categoryId, HttpServletRequest request);
    Asset getAsset(Long id, HttpServletRequest request);
    Asset createAsset(AssetController.AssetRequest body, HttpServletRequest request);
    Asset updateAsset(Long id, AssetController.AssetRequest body, HttpServletRequest request);
    Asset updateStatus(Long id, AssetController.StatusRequest body, HttpServletRequest request);
    Asset assign(Long id, AssetController.AssignRequest body, HttpServletRequest request);
    List<AssetStatusHistory> history(Long id, HttpServletRequest request);
    MaintenanceTicket report(AssetController.ReportRequest body, HttpServletRequest request);
    List<MaintenanceTicket> myTickets(HttpServletRequest request);
    Map<String, Object> listTickets(String status, Long assetId, HttpServletRequest request);
    MaintenanceTicket updateTicket(Long id, AssetController.TicketUpdateRequest body, HttpServletRequest request);
}
