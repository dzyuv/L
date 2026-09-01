package com.lab.user;

import com.lab.common.api.Roles;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Built-in permission catalog and default role bindings. */
public final class PermissionCatalog {
    public record Item(String code, String name, String group) {}

    public static final List<Item> ALL = List.of(
            new Item("resource:read", "查看可预约资源", "资源"),
            new Item("resource:manage", "管理实验室资源", "资源"),
            new Item("resource:schedule", "配置开放时间", "资源"),
            new Item("resource:closure", "维护与临时关闭", "资源"),
            new Item("resource:manager", "配置资源负责人", "资源"),
            new Item("resource-type:manage", "管理资源类别", "资源"),
            new Item("booking:create", "创建预约", "预约"),
            new Item("booking:read:self", "查看个人预约", "预约"),
            new Item("booking:cancel:self", "取消个人预约", "预约"),
            new Item("booking:checkin", "预约签到", "预约"),
            new Item("booking:read:admin", "查看全部预约", "预约"),
            new Item("approval:read", "查看待审批任务", "审批"),
            new Item("approval:decide", "审批通过或驳回", "审批"),
            new Item("asset:read", "查看资产台账", "资产"),
            new Item("asset:manage", "管理资产与入库", "资产"),
            new Item("asset:import", "批量导入资产", "资产"),
            new Item("maintenance:create", "提交报修", "报修"),
            new Item("maintenance:read:self", "查看个人报修", "报修"),
            new Item("maintenance:manage", "处理报修工单", "报修"),
            new Item("violation:manage", "处理违约记录", "违约"),
            new Item("user:read", "查看用户", "用户与权限"),
            new Item("user:manage", "管理用户账号", "用户与权限"),
            new Item("user:import", "导入用户", "用户与权限"),
            new Item("role:manage", "配置角色权限", "用户与权限"),
            new Item("config:manage", "修改系统配置", "系统"),
            new Item("log:read", "查看管理员日志", "系统")
    );

    public static final Set<String> SYSTEM_ADMIN_REQUIRED = Set.of(
            "user:read", "user:manage", "user:import", "role:manage", "config:manage", "log:read");

    private static final Map<String, List<String>> DEFAULTS = Map.of(
            Roles.STUDENT, List.of(
                    "resource:read", "booking:create", "booking:read:self", "booking:cancel:self",
                    "booking:checkin", "maintenance:create", "maintenance:read:self"),
            Roles.TEACHER, List.of(
                    "resource:read", "booking:create", "booking:read:self", "booking:cancel:self",
                    "booking:checkin", "maintenance:create", "maintenance:read:self",
                    "approval:read", "approval:decide"),
            Roles.LAB_ADMIN, List.of(
                    "resource:read", "resource:manage", "resource:schedule", "resource:closure",
                    "resource:manager", "resource-type:manage", "booking:read:admin",
                    "approval:read", "approval:decide", "asset:read", "asset:manage", "asset:import",
                    "maintenance:manage", "violation:manage"),
            Roles.SYSTEM_ADMIN, List.of(
                    "user:read", "user:manage", "user:import", "role:manage", "config:manage", "log:read")
    );

    private PermissionCatalog() {}

    public static List<String> defaultCodes(String roleCode) {
        return DEFAULTS.getOrDefault(roleCode, List.of());
    }

    public static Set<String> requiredCodes(String roleCode) {
        return Roles.SYSTEM_ADMIN.equals(roleCode) ? SYSTEM_ADMIN_REQUIRED : Set.of();
    }
}
