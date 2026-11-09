# 实验室设备预约管理系统

基于 Spring Cloud Alibaba 的实验室设备/资源预约系统，包含用户、资源、预约与审批、系统管理四个业务服务，统一由网关对外提供 API，前端使用 Vue 3。

## 技术栈

- Java 17、Spring Boot 3.3.5、Spring Cloud 2023.0.3、Spring Cloud Alibaba 2023.0.3.2
- MyBatis-Plus、MySQL 8、Nacos
- Vue 3 + Vite + Axios

## 目录结构

```
common/               公共响应体、鉴权、异常处理、持久化、可观测性配置
gateway/              Spring Cloud Gateway，统一入口（8080）
user-service/         用户、角色、权限、登录（8081）
resource-service/     资源、资产、维修、闭馆（8082）
booking-service/      预约、签到、违约、配额、审批（8083）
system-service/       系统配置、操作日志（8087）
frontend/src/         Vue 3 前端（components/ 存放页面面板）
database/             数据库初始化脚本与演示数据
database/manual/      手动 schema.sql / reset.sql
database/samples/     导入示例 CSV
deploy/               Dockerfile.service、Prometheus/Grafana 配置
docs/                 需求、设计、数据流文档
```

## 环境要求

- JDK 17 或 21（项目以 Java 17 为目标版本）
- Maven 3.9+
- Node.js 20+、npm 10+
- Docker（用于 MySQL、Nacos 等基础设施）

## 快速开始

### 1. 配置环境变量

在仓库根目录创建 `.env`（该文件已被 Git 忽略，切勿提交）：

```properties
MYSQL_PASSWORD=你的数据库密码
JWT_SECRET=至少32位随机字符串
INTERNAL_SERVICE_TOKEN=至少32位随机字符串
```

可选变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_USERNAME` | `root` | 数据库用户名（Compose 内默认使用 `lab_app`） |
| `MYSQL_APP_USER` | `lab_app` | Compose 创建的应用数据库账号，密码沿用 `MYSQL_PASSWORD` |
| `MYSQL_HOST` | `localhost` | 数据库主机（容器内通常为 `mysql`） |
| `MYSQL_PORT` | `3306` | 数据库端口 |
| `NACOS_ADDR` | `127.0.0.1:8848` | Nacos 地址 |
| `NACOS_AUTH_ENABLE` | `true` | 是否开启 Nacos 鉴权 |
| `NACOS_AUTH_TOKEN` | 内置开发默认值 | Nacos 鉴权 token（Base64，解码后至少 32 字节） |
| `NACOS_AUTH_IDENTITY_KEY` | `serverIdentity` | Nacos 服务端身份 key |
| `NACOS_AUTH_IDENTITY_VALUE` | `change-me` | Nacos 服务端身份 value |
| `NACOS_USERNAME` | `nacos` | 客户端 Nacos 用户名 |
| `NACOS_PASSWORD` | `nacos` | 客户端 Nacos 密码 |
| `USER_SERVICE_URL` | `http://localhost:8081` | user-service 地址（令牌版本校验使用） |
| `SEED_DEMO_DATA` | `false` | 是否创建演示账号、演示资源与资产分类；Compose 默认开启，生产环境必须保持关闭 |
| `GATEWAY_RATE_LIMIT_PER_MINUTE` | `120` | 网关单客户端每分钟请求上限 |
| `REDIS_HOST` | `localhost` | 网关限流使用的 Redis 地址（Compose 内为 `redis`） |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REFRESH_COOKIE_SECURE` | `false` | refresh cookie 是否加 `Secure`，HTTPS 生产环境应为 `true` |
| `REFRESH_COOKIE_SAME_SITE` | `Lax` | refresh cookie 的 `SameSite` 策略，跨站部署使用 `None` + Secure |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | 链路追踪采样率，生产环境建议 `0.1` |
| `GRAFANA_ADMIN_PASSWORD` | `admin` | Grafana 管理员密码 |
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP 追踪导出地址 |
| `MYSQL_HOST_PORT` | `3307` | Compose 映射到宿主机的 MySQL 端口 |
| `NACOS_HTTP_PORT` | `8858` | Compose 映射到宿主机的 Nacos HTTP 端口 |
| `NACOS_GRPC_PORT` | `9858` | Compose 映射到宿主机的 Nacos gRPC 端口 |
| `NACOS_GRPC_TLS_PORT` | `9859` | Compose 映射到宿主机的 Nacos gRPC TLS 端口 |
| `REFRESH_TOKEN_RETENTION_DAYS` | `7` | 过期/撤销 refresh token 的保留天数 |
| `OPERATION_LOG_RETENTION_DAYS` | `90` | 管理员操作日志保留天数 |
| `SYSTEM_SERVICE_URL` | `http://localhost:8087` | system-service 地址 |
| `RESOURCE_SERVICE_URL` | `http://localhost:8082` | resource-service 地址 |
| `BOOKING_SERVICE_URL` | `http://localhost:8083` | booking-service 地址 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 允许的前端来源，逗号分隔 |

### 2. 启动基础设施

```bash
docker compose up -d mysql nacos
```

### 3. 初始化数据库

数据库初始化分为两步：

1. **建库 + 创建最小权限账号**：Compose 的 `db-init` 会自动执行 `database/init-databases.sh`；手动运行：

```bash
MYSQL_HOST=127.0.0.1 MYSQL_ROOT_PASSWORD=你的root密码 \
MYSQL_APP_USER=lab_app MYSQL_APP_PASSWORD=你的应用密码 \
sh database/init-databases.sh
```

2. **建表与后续演进**：各服务启动时由 Flyway 执行 `src/main/resources/db/migration` 下的迁移脚本。

演示数据需在表结构创建后导入（先启动一次服务，或手动执行 `database/manual/schema.sql`）：

```bash
mysql -h127.0.0.1 -P3306 -uroot -p < database/test-data.sql
```

> `database/manual/schema.sql` 保留为手动创建完整表结构的参考脚本；运行时以 Flyway 迁移为准。
> `database/manual/reset.sql` 包含 `DROP DATABASE/TABLE`，仅可在可丢弃的本地环境中手动执行。

### 4. 构建并启动后端

```bash
mvn clean package -DskipTests
java -jar user-service/target/user-service-1.0.0.jar
java -jar resource-service/target/resource-service-1.0.0.jar
java -jar booking-service/target/booking-service-1.0.0.jar
java -jar system-service/target/system-service-1.0.0.jar
java -jar gateway/target/gateway-1.0.0.jar
```

服务支持从仓库根目录或各模块目录启动，会自动读取 `.env` 或 `../.env`。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`，Vite 会将 `/api` 代理到 `http://localhost:8080`。

## Docker 镜像

`common` 是库模块，不单独构建。5 个后端服务共用一个参数化 Dockerfile `deploy/Dockerfile.service`，通过 `SERVICE`/`PORT` 构建参数区分；前端使用独立镜像。构建上下文必须是仓库根目录：

```bash
docker compose build

# 或单独构建某个后端服务：
docker build -f deploy/Dockerfile.service \
  --build-arg SERVICE=gateway --build-arg PORT=8080 \
  -t lab/gateway:1.0.0 .

docker build -f frontend/Dockerfile -t lab/frontend:1.0.0 .
```

容器以非 root 用户（UID 10001）运行，JVM 参数通过 `JAVA_OPTS` 传入。容器内连接 MySQL/Nacos 时使用服务名，并需与基础设施容器处于同一 Docker 网络：

```bash
docker run --rm -p 8081:8081 \
  -e MYSQL_HOST=mysql -e MYSQL_PORT=3306 -e MYSQL_USERNAME=lab_app \
  -e MYSQL_PASSWORD=... -e JWT_SECRET=... -e INTERNAL_SERVICE_TOKEN=... \
  -e NACOS_ADDR=nacos:8848 \
  lab/user-service:1.0.0
```

## Docker Compose 一键启动

在仓库根目录准备好 `.env`（至少包含 `MYSQL_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN`）后：

```bash
docker compose up -d --build
```

Compose 会启动 MySQL、Nacos、Redis、`db-init`、5 个应用服务、前端以及 Prometheus/Grafana/Jaeger。Redis 用于网关分布式限流。`db-init` 在 MySQL 健康后执行 `database/init-databases.sh` 创建数据库与最小权限应用账号；各服务启动时由 Flyway 创建/升级表结构。应用服务与前端都配置了健康检查，gateway 会等待业务服务和 Redis 健康后再启动。

> 从旧版本升级时，使用 `docker compose up -d --build --remove-orphans` 启动，以移除已合并的 `approval-service` 容器。Flyway V3 会把 `lab_approval` 中的审批数据复制到 `lab_booking`。

访问入口：

| 服务 | 地址 |
|---|---|
| 前端 | `http://localhost:5173` |
| 网关 | `http://localhost:8080` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000`（默认 `admin/admin`） |
| Jaeger | `http://localhost:16686` |

> 为避免与本机已有服务冲突，Compose 将 MySQL 映射到 `3307`、Nacos 映射到 `8858`；可用 `MYSQL_HOST_PORT`、`NACOS_HTTP_PORT` 等变量覆盖。

加载演示数据（可选）：

```bash
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < database/test-data.sql
```

常用命令：

```bash
docker compose ps                 # 查看状态
docker compose logs -f gateway    # 查看日志
docker compose down               # 停止并保留 MySQL 数据卷
docker compose down -v            # 停止并删除数据卷（会清空数据库）
```

## 健康检查

每个服务都引入了 Spring Boot Actuator，暴露 `health`、`info`、`prometheus`。Docker 镜像使用 `/actuator/health` 作为容器健康检查。

| 端点 | 说明 |
|---|---|
| `/actuator/health` | 总体健康状态（含数据库连通性） |
| `/actuator/health/liveness` | 存活探针 |
| `/actuator/health/readiness` | 就绪探针 |

示例：`curl http://localhost:8080/actuator/health`（业务服务端口在 Compose 中不对外发布）

## 可观测性

- 每个服务通过 `/actuator/prometheus` 暴露 Micrometer 指标。
- 日志行包含 `traceId`、`spanId`、`requestId`，便于跨服务串联。
- `TRACING_SAMPLING_PROBABILITY` 控制采样率，默认 `1.0`，生产建议 `0.1`。
- Compose 内置 Prometheus、Grafana、Jaeger；服务通过 OTLP HTTP 将追踪发送到 Jaeger。
- `MANAGEMENT_OTLP_TRACING_ENDPOINT` 可覆盖 OTLP 导出地址。

## 远程调用韧性

- 所有服务间 `RestClient` 统一使用 2 秒连接超时、5 秒读取超时。
- 网络类异常（`ResourceAccessException`）最多重试 1 次（总计 2 次尝试）。
- 使用 Resilience4j 熔断器：10 次滑动窗口、失败率 50% 打开、打开 10 秒后进入半开放。
- 管理员审计日志在数据库事务提交后发送，避免远程调用拖长业务事务。

> 资源闭馆到预约取消/恢复仍为同步调用。

## 审批

审批能力已合并到 `booking-service`。审批任务、审批记录与预约状态位于同一个数据库，并在同一本地事务中更新，不再依赖 Outbox 或跨服务回调。

## 端口

| 服务 | 端口 |
|---|---|
| gateway | 8080（对外） |
| user-service | 8081（Compose 内部） |
| resource-service | 8082（Compose 内部） |
| booking-service | 8083（Compose 内部） |
| system-service | 8087（Compose 内部） |
| frontend (dev / compose) | 5173（对外） |
| MySQL | 3306（本地）/ 3307（Compose） |
| Nacos | 8848（本地）/ 8858（Compose） |
| Redis | 6379（Compose 内部） |
| Prometheus | 9090 |
| Grafana | 3000 |
| Jaeger UI | 16686 |
| OTLP HTTP | 4318 |

> Compose 只发布 gateway、frontend 与基础设施端口；业务服务端口仅在 Docker 网络内部可达。需要调试单个服务时使用 `docker compose exec` 或 `docker compose run --service-ports`。

## 演示账号

`test-data.sql` 与 `UserDataInitializer` 使用相同密码：`12345678`。

演示账号仅在 `SEED_DEMO_DATA=true` 时自动创建；非 Compose 环境默认为关闭，生产环境必须保持关闭。`test-data.sql` 可手动导入演示数据。

| 账号 | 角色 |
|---|---|
| `S20260001` | 学生 |
| `S20260002` | 学生 |
| `T20260001` | 教师 |
| `LAB20260001` | 实验室管理员（兼教师） |
| `ADMIN20260001` | 系统管理员 |

## 安全说明

- 不要把 `.env` 提交到版本库；如果历史提交中曾包含真实密钥，请立即轮换 `MYSQL_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN`。
- 访问令牌包含 `ver` 声明，各服务通过 user-service 校验当前令牌版本（30 秒缓存）；禁用用户或调整角色后旧令牌立即失效。升级后不带 `ver` 的旧令牌会被拒绝，需要重新登录。user-service 不可用时，业务服务返回 503 而不是放行旧令牌。
- 网关默认按客户端限流 120 请求/分钟，可通过 `GATEWAY_RATE_LIMIT_PER_MINUTE` 调整，并返回 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Content-Security-Policy` 等安全响应头。限流计数默认存储在 Redis，多网关实例共享；Redis 不可用时自动回退到单实例内存限流。
- 网关限流只信任来自私有网段/回环代理的 `X-Forwarded-For`；前端 Nginx 会覆盖该头。不要把网关直接暴露给不可信网络。
- 容器内应用使用 `lab_app` 账号，仅授权五个业务库，不再使用 root；`MYSQL_PASSWORD` 同时作为 Compose 中 root 和应用账号密码，生产环境应拆分为不同密码。
- 内部服务调用通过 `X-Internal-Service-Token` 校验，令牌长度不得少于 32 字符。
- 轮换密钥可使用 `openssl rand -base64 48` 生成新值，更新 `.env` 后重启服务；清理 Git 历史中的旧密钥属于不可逆操作，需要 `git filter-repo`/BFG 等工具并单独确认。
- Compose 默认开启 Nacos 鉴权；`nacos/nacos` 登录账号以及默认的 `NACOS_AUTH_TOKEN`/identity 仅用于本地开发，生产环境必须替换。可用 `openssl rand -base64 32` 生成新的 `NACOS_AUTH_TOKEN`，并在 Nacos 控制台修改默认密码。
- 前端 access token 仅保存在内存中；refresh token 使用 httpOnly Cookie（默认 `SameSite=Lax`，HTTPS 生产环境建议 `REFRESH_COOKIE_SECURE=true`），页面刷新时自动通过 Cookie 恢复会话，401 时自动刷新并重试原请求。

## 运维任务

- refresh token 默认保留 7 天，操作日志默认保留 90 天，由 user-service / system-service 的定时任务清理；可用 `REFRESH_TOKEN_RETENTION_DAYS`、`OPERATION_LOG_RETENTION_DAYS` 调整。
- booking-service 的自动完成、爽约、审批超时任务使用 ShedLock，多实例部署时只会有一个实例执行。

## 持续集成

`.github/workflows/ci.yml` 在 push 到 `main`、提交 PR 或手动触发时运行：

- 后端：`mvn -B verify`（Java 17，运行单元测试、Testcontainers 集成测试并打包）
- 前端：`npm ci && npm run build`
- Docker：矩阵构建 5 个后端服务 + 前端镜像（不推送）

## 测试

```bash
mvn test
```

当前单元测试覆盖：鉴权、错误信息、预约生命周期，以及创建预约时的配额上限、时间冲突、日时长限制与资源占用冲突。

集成测试使用 Testcontainers + MySQL 验证真实 SQL 约束与持久化行为；Docker 不可用时自动跳过。

## 已知待办

- 现有单元测试覆盖创建预约的配额/冲突规则；Testcontainers 集成测试覆盖预约持久化、时段唯一约束与审批任务持久化，审批状态机等跨服务流程仍需补充集成测试。
- 列表接口支持 `page`/`size` 参数，默认最多返回 1000 条；前端管理页目前仍使用客户端分页。
- 预约创建仍在事务内调用 resource-service。
- 资源闭馆到预约取消/恢复仍为同步调用。
- 服务间调用默认指向 `localhost`，容器化部署时需要设置上表中的 `*_SERVICE_URL`。
