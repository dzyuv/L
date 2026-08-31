const ERROR_MESSAGES = {
  LOGIN_FAILED: "账号或密码错误",
  USER_DISABLED: "账号当前不可用，请联系系统管理员",
  USER_EXISTS: "该工号已经注册",
  USER_NOT_FOUND: "用户不存在",
  INVALID_PASSWORD: "密码长度必须为 8 至 72 位",
  REFRESH_TOKEN_INVALID: "登录凭证无效或已过期，请重新登录",
  UNAUTHORIZED: "请先登录后再操作",
  FORBIDDEN: "当前账号没有执行此操作的权限",
  INTERNAL_AUTH_REQUIRED: "服务身份验证失败",
  INVALID_ARGUMENT: "提交的信息不完整或格式不正确",
  INVALID_TIME: "开始时间必须早于结束时间",
  INVALID_DATE_RANGE: "日期范围不正确",
  INVALID_STATUS: "当前状态不允许执行此操作",
  INVALID_ACTION: "不支持该操作",
  NOT_FOUND: "请求的数据不存在或已被删除",
  INTERNAL_ERROR: "系统处理失败，请稍后重试",
  SERVICE_UNAVAILABLE: "服务暂时不可用，请稍后重试",
  RESOURCE_SERVICE_UNAVAILABLE: "资源服务暂时不可用，请稍后重试",
  BOOKING_SERVICE_UNAVAILABLE: "预约服务暂时不可用，请稍后重试",
  APPROVAL_SERVICE_UNAVAILABLE: "审批服务暂时不可用，请稍后重试",
  RESOURCE_NOT_FOUND: "资源不存在或已被删除",
  TYPE_NOT_FOUND: "资源类别不存在或已被删除",
  TYPE_EXISTS: "资源类别名称已存在",
  TYPE_IN_USE: "该类别仍关联资源，不能删除",
  RESOURCE_UNAVAILABLE: "该资源当前不可预约",
  RESOURCE_CLOSED: "该时段资源处于维护或临时关闭状态",
  OUTSIDE_OPEN_HOURS: "所选时间不在资源开放时间内",
  CAPACITY_EXCEEDED: "预约人数超过资源容量",
  INVALID_SCHEDULE: "开放时间设置不正确",
  INVALID_CLOSURE: "维护关闭时间设置不正确",
  ESCALATION_APPROVER_NOT_CONFIGURED: "未配置可处理该申请的上级审批人",
  RESOURCE_OWNER_REQUIRED: "请先为该资源配置资源负责人",
  MANAGER_EXISTS: "该用户已经是此资源的负责人",
  IDEMPOTENCY_REQUIRED: "请勿重复提交，请刷新后重试",
  BOOKING_CONFLICT: "所选时段已被占用，请重新选择",
  USER_TIME_CONFLICT: "你在该时段已有其他预约",
  BOOKING_ACTIVE_LIMIT: "未结束的预约数量已达到上限",
  BOOKING_PENDING_LIMIT: "待审批预约数量已达到上限",
  BOOKING_RESOURCE_LIMIT: "该资源的未结束预约数量已达到上限",
  BOOKING_DAILY_DURATION_LIMIT: "当天累计预约时长已达到上限",
  BOOKING_ADVANCE_LIMIT: "预约日期超出允许的提前预约范围",
  CHECKIN_WINDOW: "当前不在允许签到的时间范围内",
  VIOLATION_ALREADY_PROCESSED: "该违约记录已经处理",
  VIOLATION_REASON_REQUIRED: "撤销违约时必须填写原因",
  USER_RESTRICTED: "当前账号暂时不能创建预约",
  APPROVAL_EXPIRED: "审批任务已过期",
  BOOKING_NOT_PENDING: "该预约已取消或已超时，审批任务已关闭",
  BOOKING_CANCELED: "该预约已被申请人取消",
  BOOKING_EXPIRED: "该预约已因超时失效",
  REJECTION_REASON_REQUIRED: "驳回时必须填写原因",
  SELF_APPROVAL_FORBIDDEN: "不能审批自己提交的预约",
  TASK_ALREADY_COMPLETED: "该审批任务已经处理",
  ROLE_NOT_FOUND: "所选角色不存在",
  INVALID_ROLE: "请选择有效的用户角色",
  SELF_DISABLE_FORBIDDEN: "不能停用当前登录账号",
  SELF_DELETE_FORBIDDEN: "不能删除当前登录账号",
  SELF_ROLE_CHANGE_FORBIDDEN: "不能移除当前账号的系统管理员角色",
  CATEGORY_EXISTS: "分类名称已存在",
  ASSET_EXISTS: "资产编号或序列号已存在",
  ASSET_NO_REQUIRED: "未开启自动编号时必须填写资产编号",
  SERIAL_NO_REQUIRED: "序列化或贵重资产必须填写唯一序列号",
  ASSET_UNAVAILABLE: "该资产当前不能提交报修",
  ASSET_REQUIRED: "开始维修前必须绑定具体资产",
  OPEN_TICKET_EXISTS: "该资产已有未结束的报修工单",
  LOCATION_REQUIRED: "请选择资源或填写问题发生位置",
  INVALID_REPORT_TYPE: "报修类型不正确",
  INVALID_SEVERITY: "严重程度不正确",
  INVALID_TRANSITION: "当前工单状态不能进行此变更",
  INVALID_PHONE: "请填写有效的维修负责人电话",
};

export function localizeApiError(error) {
  const code = error?.response?.data?.code;
  const serverMessage = String(error?.response?.data?.message || "").trim();
  if (ERROR_MESSAGES[code]) return ERROR_MESSAGES[code];
  if (/\p{Script=Han}/u.test(serverMessage)) return serverMessage;
  if (!error?.response) return "网络连接失败，请检查网络后重试";
  if (error.response.status >= 500) return "系统服务暂时不可用，请稍后重试";
  return "操作未完成，请检查填写内容后重试";
}

export function installChineseErrorMessages(axios) {
  axios.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error?.response?.data && typeof error.response.data === "object") {
        error.response.data.message = localizeApiError(error);
      }
      return Promise.reject(error);
    },
  );
}
