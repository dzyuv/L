 # 实验室设备预约管理系统测试手册

版本：1.0  
适用版本：当前开发基线  
测试日期：__________  
测试人员：__________

## 1. 测试范围

本手册用于验证当前版本的以下功能：

- Vue 登录页和预约工作台
- 用户注册、登录、JWT 访问令牌和当前用户查询
- 资源类型、资源、开放时间和资源日历
- 预约创建、时间校验、容量校验、幂等提交和时间片冲突
- 预约查询、取消、签到、自动完成和未签到处理
- 资源维护/关闭区间
- 审批任务查询、通过和驳回
- 统一响应格式、请求 ID、错误码和基础鉴权

以下功能在当前版本属于开发占位或简化实现，测试时不要按生产完成度验收：完整多级审批自动编排、二维码签到。

## 2. 环境要求

| 组件 | 要求 |
|---|---|
| 操作系统 | Windows 10/11 或同等环境 |
| Java | 17 及以上 |
| Maven | 3.9 及以上 |
| Node.js | 20.x |
| npm | 10.x |
| MySQL | 8.0，端口 3306 |
| Redis | 7.x，端口 6379（当前核心流程可选） |
| 浏览器 | Chrome/Edge 最新版 |

默认数据库账号：`root`，密码：`111111`。如果密码不同，请修改各服务 `application.yml`。

MySQL 使用 Docker 命名卷 `d_mysql_data` 持久化。可以重建容器，但不要执行 `docker compose down -v`，否则会删除测试数据。

## 3. 启动系统

### 3.1 启动 MySQL 和 Redis

在项目根目录执行：

```powershell
docker compose up -d mysql redis
docker compose ps
```

确认 MySQL 状态为 `running`，端口 3306 可连接。

### 3.2 编译后端

```powershell
mvn -DskipTests package
```

预期：输出 `BUILD SUCCESS`，每个服务生成 `target/*.jar`。

### 3.3 启动后端服务

建议使用多个终端分别启动：

```powershell
java -jar user-service/target/user-service-1.0.0.jar
java -jar resource-service/target/resource-service-1.0.0.jar
java -jar booking-service/target/booking-service-1.0.0.jar
java -jar approval-service/target/approval-service-1.0.0.jar
java -jar system-service/target/system-service-1.0.0.jar
java -jar gateway/target/gateway-1.0.0.jar
```

服务端口：

| 服务 | 端口 |
|---|---:|
| Gateway | 8080 |
| user-service | 8081 |
| resource-service | 8082 |
| booking-service | 8083 |
| approval-service | 8084 |
| system-service | 8087 |

### 3.4 启动前端

```powershell
cd frontend
npm install
npm run dev -- --host 127.0.0.1
```

访问：`http://127.0.0.1:5173`

## 4. 初始化测试数据

### 4.1 注册测试用户

PowerShell：

```powershell
$body = @{ employeeNo='S20260001'; realName='张三'; password='12345678'; email='zhangsan@example.com'; phone='13800000000' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/user/register -ContentType 'application/json' -Body $body
```

预期：

- HTTP 200
- `code` 为 `SUCCESS`
- 返回用户角色为 `STUDENT`
- 再次使用相同 `employeeNo` 注册，预期 HTTP 409，错误码 `USER_EXISTS`

### 4.2 登录并保存令牌

```powershell
$login = @{ username='S20260001'; password='12345678' } | ConvertTo-Json
$result = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/user/login -ContentType 'application/json' -Body $login
$token = $result.data.accessToken
$headers = @{ Authorization="Bearer $token" }
```

登录成功后，后续受保护请求必须带：

```text
Authorization: Bearer <accessToken>
```

上面这一行只是 HTTP 请求头示例，不能直接作为 PowerShell 命令执行。PowerShell 中请使用前面创建的 `$headers` 变量。如果中文显示为乱码，可先执行 `chcp 65001`，或使用 PowerShell 7；这属于终端编码显示问题，不代表数据库内容损坏。

### 4.3 创建资源类型

当前资源管理接口没有管理员前端页面，使用接口初始化：

```powershell
$body = @{ name='实验室'; defaultApprovalLevel=1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/admin/resource-types -Headers $headers -ContentType 'application/json' -Body $body
```

记录返回的类型 ID，例如 `1`。

### 4.4 创建资源

```powershell
$body = @{ typeId=1; name='计算机实验室 A101'; location='一号楼 101'; capacity=30; description='用于课程实验'; maxDurationMinutes=120; needCheckin=$true } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/admin/resources -Headers $headers -ContentType 'application/json' -Body $body
```

记录资源 ID，例如 `1`。

### 4.5 配置开放时间

以周一 09:00-17:00、30 分钟粒度为例：

```powershell
$body = @(
  @{ weekday=1; openTime='09:00:00'; closeTime='17:00:00'; slotMinutes=30; maxDurationMinutes=120 },
  @{ weekday=2; openTime='09:00:00'; closeTime='17:00:00'; slotMinutes=30; maxDurationMinutes=120 }
) | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri http://127.0.0.1:8080/api/v1/admin/resources/1/schedules -Headers $headers -ContentType 'application/json' -Body $body
```

## 5. 统一接口检查

任意接口都应检查以下字段：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "requestId": "..."
}
```

检查项：

1. 响应包含 `code`、`message`、`data`、`requestId`。
2. 请求带 `X-Request-Id: test-request-001` 时，响应中的 `requestId` 或响应头应保留该值。
3. 不带令牌访问受保护接口，预期 HTTP 401。
4. 无效或过期令牌，预期 HTTP 401。
5. 不存在的资源或预约，预期 HTTP 404。
6. 时间片冲突，预期 HTTP 409。
7. 状态不允许的操作，预期 HTTP 422。

## 6. 用户和认证测试

| 编号 | 操作 | 预期结果 | 结果 |
|---|---|---|---|
| U-01 | 注册合法用户 | 创建 STUDENT 用户 | □通过 □失败 |
| U-02 | 重复学号/工号注册 | 409，`USER_EXISTS` | □通过 □失败 |
| U-03 | 密码少于 8 位 | 参数校验失败 | □通过 □失败 |
| U-04 | 错误密码登录 | 401，不能返回令牌 | □通过 □失败 |
| U-05 | 连续错误密码 5 次 | 账号进入 LOCKED | □通过 □失败 |
| U-06 | 正确密码登录 | 返回 accessToken | □通过 □失败 |
| U-07 | 不带 Authorization 调用 `/user/me` | 401 | □通过 □失败 |
| U-08 | 使用令牌调用 `/user/me` | 返回当前登录用户 | □通过 □失败 |
| U-09 | 修改请求中的 `X-User-Id` | 服务端仍按 JWT 用户处理 | □通过 □失败 |

## 7. 资源和日历测试

| 编号 | 操作 | 预期结果 | 结果 |
|---|---|---|---|
| R-01 | 查询资源列表 | 返回资源数组 | □通过 □失败 |
| R-02 | 查询不存在资源 | 404 | □通过 □失败 |
| R-03 | 创建资源类型 | 创建成功 | □通过 □失败 |
| R-04 | 使用不存在的类型创建资源 | 404，`TYPE_NOT_FOUND` | □通过 □失败 |
| R-05 | 配置合法开放时间 | 保存成功 | □通过 □失败 |
| R-06 | `openTime >= closeTime` | 400，`INVALID_SCHEDULE` | □通过 □失败 |
| R-07 | 查询日历 | 返回日期、开放时间和资源信息 | □通过 □失败 |
| R-08 | 创建维护关闭区间 | 返回关闭记录 | □通过 □失败 |
| R-09 | 取消关闭区间 | 状态变为 `CANCELED` | □通过 □失败 |

## 8. 预约核心测试

### 8.1 创建预约请求

以下请求可用于资源 ID 为 1 的测试：

```powershell
$body = @{ resourceId=1; resourceName='计算机实验室 A101'; startTime='2026-09-07T09:00:00'; endTime='2026-09-07T10:00:00'; purpose='课程实验'; participants=3; capacity=30; slotMinutes=30; maxDurationMinutes=120; needCheckin=$true; approvalLevel=1 } | ConvertTo-Json
$booking = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/v1/bookings -Headers ($headers + @{ 'Idempotency-Key'='booking-test-001' }) -ContentType 'application/json' -Body $body
$booking.data
```

### 8.2 用例表

| 编号 | 场景 | 预期结果 | 结果 |
|---|---|---|---|
| B-01 | 合法创建 09:00-10:00 预约 | 创建成功，状态为 `PENDING_APPROVAL` | □通过 □失败 |
| B-02 | 同一 `Idempotency-Key` 重复提交 | 返回第一次预约，不新增记录 | □通过 □失败 |
| B-03 | 不带 `Idempotency-Key` | 400，`IDEMPOTENCY_REQUIRED` | □通过 □失败 |
| B-04 | 开始时间晚于结束时间 | 400，`INVALID_TIME` | □通过 □失败 |
| B-05 | 预约时长不是粒度整数倍 | 400，`INVALID_TIME` | □通过 □失败 |
| B-06 | 开始分钟不对齐 30 分钟粒度 | 400，`INVALID_TIME` | □通过 □失败 |
| B-07 | 参与人数大于容量 | 400，`CAPACITY_EXCEEDED` | □通过 □失败 |
| B-08 | 同资源同时间再次预约 | 409，`BOOKING_CONFLICT` | □通过 □失败 |
| B-09 | 10:00-11:00 紧接 09:00-10:00 | 创建成功，验证左闭右开 | □通过 □失败 |
| B-10 | 查询我的预约 | 只返回当前 JWT 用户的预约 | □通过 □失败 |
| B-11 | 其他用户取消该预约 | 403，`FORBIDDEN` | □通过 □失败 |
| B-12 | 申请人取消未开始预约 | 状态变为 `CANCELED`，时间片释放 | □通过 □失败 |
| B-13 | 取消已开始或已签到预约 | 422，`INVALID_STATUS` | □通过 □失败 |
| B-14 | APPROVED 预约在签到窗口内签到 | 状态变为 `CHECKED_IN` | □通过 □失败 |
| B-15 | 非签到窗口签到 | 422，`CHECKIN_WINDOW` | □通过 □失败 |
| B-16 | needCheckin=false 且结束时间已到 | 自动变为 `COMPLETED` | □通过 □失败 |
| B-17 | needCheckin=true 且超过开始后 30 分钟未签到 | 自动变为 `NO_SHOW` | □通过 □失败 |

## 9. 并发冲突测试

目标：验证数据库时间片唯一索引是最终正确性保障。

准备同一个资源、同一时间段，生成两个不同的 `Idempotency-Key`，同时发送两个 POST 请求。

预期：

- 最多一个请求成功。
- 另一个请求返回 HTTP 409 和 `BOOKING_CONFLICT`。
- 数据库中只有一条有效预约和对应时间片。
- 不允许出现两个预约都成功的情况。

PowerShell 可使用两个后台任务：

```powershell
$jobs = 1..2 | ForEach-Object {
  Start-Job -ScriptBlock { param($body,$key) Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8080/api/v1/bookings' -Headers @{Authorization=$using:token;'Idempotency-Key'=$key} -ContentType 'application/json' -Body $body } -ArgumentList $body,"concurrent-$($_)"
}
$jobs | Wait-Job | Receive-Job
```

## 10. 审批测试

当前审批任务可以通过数据库或接口准备测试数据。验证以下行为：

| 编号 | 操作 | 预期结果 | 结果 |
|---|---|---|---|
| A-01 | 查询我的待审批任务 | 返回 `PENDING` 任务 | □通过 □失败 |
| A-02 | 合法通过任务 | 状态变为 `APPROVED` | □通过 □失败 |
| A-03 | 合法驳回任务 | 状态变为 `REJECTED` | □通过 □失败 |
| A-04 | 重复处理已完成任务 | 422，不能重复处理 | □通过 □失败 |
| A-05 | 非法 action | 400，`INVALID_ACTION` | □通过 □失败 |
| A-06 | 填写审批意见 | 记录 `comment` | □通过 □失败 |

注意：当前版本审批服务尚未通过 RabbitMQ 自动把审批结果同步回 booking-service，审批接口本身可测试，但跨服务最终状态联动应标记为待实现。

## 11. 前端界面测试

访问 `http://127.0.0.1:5173`，使用已注册账号登录。

### 11.1 登录页

- 页面首屏布局完整，品牌、登录表单和功能说明无重叠。
- 用户名和密码为空时，浏览器输入校验生效。
- 正确账号可以进入工作台。
- 错误密码显示失败提示，不应进入工作台。
- 刷新浏览器后，已登录状态可以恢复；令牌无效时应重新登录。
- 375px 手机宽度下内容不横向溢出。

### 11.2 工作台

- 资源数量、我的预约数量正常显示。
- 资源列表可选中，选中状态明显。
- 选择资源后容量、最大预约时长等字段更新。
- 开始时间、结束时间、用途、参与人数输入正常。
- 未选择资源时提交按钮不可用。
- 提交成功后显示提示并刷新预约列表。
- 重复点击提交不会因相同幂等键产生重复记录；当前界面每次提交生成新键，接口测试需单独验证固定键。
- 可取消允许取消的预约。
- 状态颜色和状态文字可读，表格在移动端不遮挡。

### 11.3 浏览器兼容和视觉检查

分别使用 Chrome 和 Edge 检查：

- 100%、125%、150% 缩放下布局无重叠。
- 1440px、1024px、768px、375px 宽度下无横向滚动条。
- 键盘 Tab 可以访问表单和按钮。
- 输入框聚焦状态清晰。
- 错误提示不遮挡其他表单。
- 刷新按钮有可识别的 tooltip。

## 12. 数据库检查

登录 MySQL 后可执行：

```sql
SHOW DATABASES LIKE 'lab_%';
USE lab_booking;
SELECT booking_no, user_id, resource_id, start_time, end_time, status FROM booking;
SELECT resource_id, booking_id, slot_start FROM booking_slot ORDER BY slot_start;
```

重点检查：

1. `booking.client_request_id` 唯一。
2. `booking_slot(resource_id, slot_start)` 唯一。
3. 取消、完成、未签到后，对应时间片已删除或释放，不能继续阻止新预约。
4. 预约和时间片创建失败时没有残留半条数据。

## 13. 缺陷记录模板

| 字段 | 内容 |
|---|---|
| 缺陷编号 | BUG-____ |
| 发现时间 | |
| 测试人员 | |
| 环境 | 浏览器/操作系统/服务版本 |
| 前置条件 | |
| 重现步骤 | 1.  2.  3. |
| 实际结果 | |
| 预期结果 | |
| 严重程度 | 致命 / 严重 / 一般 / 建议 |
| 截图或日志 | |
| 是否阻塞验收 | 是 / 否 |

## 14. 验收结论

建议满足以下条件后再判定当前版本通过：

- U-01、U-04、U-06、U-07 全部通过。
- R-01、R-05、R-07 全部通过。
- B-01、B-02、B-08、B-09、B-12、B-14 全部通过。
- 并发测试中不能出现两个相同时间片同时成功。
- 登录页和工作台在桌面、移动端均无明显布局错误。
- 所有未实现的跨服务能力已登记为后续开发项，而不是作为已完成能力验收。

测试结论：□通过  □有条件通过  □不通过  
测试负责人签字：________________  日期：________________
