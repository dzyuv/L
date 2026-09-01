package com.lab.common.exception;

import java.util.Map;

/** Keeps API error messages suitable for direct display in the Chinese UI. */
public final class UserMessageLocalizer {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("LOGIN_FAILED", "账号或密码错误"),
            Map.entry("USER_DISABLED", "账号当前不可用，请联系系统管理员"),
            Map.entry("USER_EXISTS", "该工号已经注册"),
            Map.entry("USER_NOT_FOUND", "用户不存在"),
            Map.entry("INVALID_PASSWORD", "密码长度必须为 8 至 72 位"),
            Map.entry("REFRESH_TOKEN_INVALID", "登录凭证无效或已过期，请重新登录"),
            Map.entry("UNAUTHORIZED", "请先登录后再操作"),
            Map.entry("FORBIDDEN", "当前账号没有执行此操作的权限"),
            Map.entry("INTERNAL_AUTH_REQUIRED", "服务身份验证失败"),
            Map.entry("INVALID_ARGUMENT", "提交的信息不完整或格式不正确"),
            Map.entry("INVALID_TIME", "开始时间必须早于结束时间"),
            Map.entry("BOOKING_NOT_SAME_DAY", "预约必须在同一天内"),
            Map.entry("BOOKING_START_IN_PAST", "预约开始时间必须晚于当前时间"),
            Map.entry("INVALID_SLOT", "所选时段不符合开放粒度和最长预约时长"),
            Map.entry("INVALID_DATE_RANGE", "日期范围不正确"),
            Map.entry("INVALID_STATUS", "当前状态不允许执行此操作"),
            Map.entry("INVALID_ACTION", "不支持该操作"),
            Map.entry("NOT_FOUND", "请求的数据不存在或已被删除"),
            Map.entry("INTERNAL_ERROR", "系统处理失败，请稍后重试"),
            Map.entry("SERVICE_UNAVAILABLE", "服务暂时不可用，请稍后重试"),
            Map.entry("RESOURCE_SERVICE_UNAVAILABLE", "资源服务暂时不可用，请稍后重试"),
            Map.entry("BOOKING_SERVICE_UNAVAILABLE", "预约服务暂时不可用，请稍后重试"),
            Map.entry("APPROVAL_SERVICE_UNAVAILABLE", "审批服务暂时不可用，请稍后重试"),
            Map.entry("RESOURCE_NOT_FOUND", "资源不存在或已被删除"),
            Map.entry("TYPE_NOT_FOUND", "资源类别不存在或已被删除"),
            Map.entry("TYPE_EXISTS", "资源类别名称已存在"),
            Map.entry("TYPE_IN_USE", "该类别仍关联资源，不能删除"),
            Map.entry("RESOURCE_UNAVAILABLE", "该资源当前不可预约"),
            Map.entry("RESOURCE_CLOSED", "该时段资源处于维护或临时关闭状态"),
            Map.entry("OUTSIDE_OPEN_HOURS", "所选时间不在资源开放时间内"),
            Map.entry("CAPACITY_EXCEEDED", "预约人数超过资源容量"),
            Map.entry("INVALID_SCHEDULE", "开放时间设置不正确"),
            Map.entry("INVALID_CLOSURE", "维护关闭时间设置不正确"),
            Map.entry("ESCALATION_APPROVER_NOT_CONFIGURED", "未配置可处理该申请的上级审批人"),
            Map.entry("RESOURCE_OWNER_REQUIRED", "请先为该资源配置资源负责人"),
            Map.entry("MANAGER_EXISTS", "该用户已经是此资源的负责人"),
            Map.entry("IDEMPOTENCY_REQUIRED", "请勿重复提交，请刷新后重试"),
            Map.entry("BOOKING_CONFLICT", "所选时段已被占用，请重新选择"),
            Map.entry("USER_TIME_CONFLICT", "你在该时段已有其他预约"),
            Map.entry("BOOKING_ACTIVE_LIMIT", "未结束的预约数量已达到上限"),
            Map.entry("BOOKING_PENDING_LIMIT", "待审批预约数量已达到上限"),
            Map.entry("BOOKING_RESOURCE_LIMIT", "该资源的未结束预约数量已达到上限"),
            Map.entry("BOOKING_DAILY_DURATION_LIMIT", "当天累计预约时长已达到上限"),
            Map.entry("BOOKING_ADVANCE_LIMIT", "预约日期超出允许的提前预约范围"),
            Map.entry("CHECKIN_WINDOW", "当前不在允许签到的时间范围内"),
            Map.entry("VIOLATION_ALREADY_PROCESSED", "该违约记录已经处理"),
            Map.entry("VIOLATION_REASON_REQUIRED", "撤销违约时必须填写原因"),
            Map.entry("USER_RESTRICTED", "当前账号暂时不能创建预约"),
            Map.entry("APPROVAL_EXPIRED", "审批任务已过期"),
            Map.entry("BOOKING_NOT_PENDING", "该预约已取消或已超时，审批任务已关闭"),
            Map.entry("BOOKING_CANCELED", "该预约已被申请人取消"),
            Map.entry("BOOKING_EXPIRED", "该预约已因超时失效"),
            Map.entry("REJECTION_REASON_REQUIRED", "驳回时必须填写原因"),
            Map.entry("SELF_APPROVAL_FORBIDDEN", "不能审批自己提交的预约"),
            Map.entry("TASK_ALREADY_COMPLETED", "该审批任务已经处理"),
            Map.entry("ROLE_NOT_FOUND", "所选角色不存在"),
            Map.entry("INVALID_ROLE", "请选择有效的用户角色"),
            Map.entry("SELF_DISABLE_FORBIDDEN", "不能停用当前登录账号"),
            Map.entry("SELF_DELETE_FORBIDDEN", "不能删除当前登录账号"),
            Map.entry("SELF_ROLE_CHANGE_FORBIDDEN", "不能移除当前账号的系统管理员角色"),
            Map.entry("CATEGORY_EXISTS", "分类名称已存在"),
            Map.entry("ASSET_EXISTS", "资产编号或序列号已存在"),
            Map.entry("SERIAL_NO_REQUIRED", "序列化或贵重资产必须填写唯一序列号"),
            Map.entry("ASSET_UNAVAILABLE", "该资产当前不能提交报修"),
            Map.entry("ASSET_REQUIRED", "开始维修前必须绑定具体资产"),
            Map.entry("OPEN_TICKET_EXISTS", "该资产已有未结束的报修工单"),
            Map.entry("LOCATION_REQUIRED", "请选择资源或填写问题发生位置"),
            Map.entry("INVALID_REPORT_TYPE", "报修类型不正确"),
            Map.entry("INVALID_SEVERITY", "严重程度不正确"),
            Map.entry("INVALID_TRANSITION", "当前工单状态不能进行此变更"),
            Map.entry("INVALID_PHONE", "请填写有效的维修负责人电话")
    );

    private UserMessageLocalizer() {}

    public static String resolve(String code, String original) {
        String localized = MESSAGES.get(code);
        if (localized != null) return localized;
        if (original != null && original.codePoints().anyMatch(value -> value >= 0x3400 && value <= 0x9fff)) {
            return original;
        }
        return "操作未完成，请稍后重试";
    }
}
