# 图书管理系统（Spring Boot 3 + Vue 3 前后端分离）

> 技术栈：Spring Boot 3.2.5 / MyBatis-Plus 3.5.5 / MySQL 8.0 / Spring Security + JWT / Spring AOP（审计日志）
> / Logback + MDC（JSON 结构化日志、traceId 串联）/ Flyway（迁移）/ @Scheduled（逾期扫描、预约清扫、到期提醒）
> / Vue 3 + Vite 5 + Element Plus + Pinia / ECharts 6（统计图表）
>
> **运行状态：本机已实测跑通**（Java 17.0.8、Maven 3.6.1、Node 22、MySQL 8.0.33）。
> 后端接口自检 13 项 + 写接口回归 17 项全部通过；测试 61 项（58 个单测通过，3 个 Testcontainers 集成用例本机无 Docker 自动跳过）；
> 前端 `npm run build` 与 `eslint` 均通过，统计页 / 操作日志页 / 借阅管理页已用无头 Chrome 截图验证，控制台无报错。
>
> **工程规范**：`.editorconfig` + `.gitattributes` 统一编码换行、ESLint + Prettier 静态检查、
> JUnit 5 + Mockito 单元测试 + Testcontainers 集成测试、springdoc-openapi 接口文档、actuator 健康检查、
> GitHub Actions CI、Conventional Commits 提交校验（`git config core.hooksPath scripts/hooks`）。

---

## 一、最快跑起来（本机直接运行，推荐）

### 前置条件

| 依赖 | 版本要求 | 检查命令 |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| MySQL | 8.0，已启动在 3306 | `mysql --version` |

### 三步启动

**① 数据库：不用手工建表了**

表结构由后端启动时的 **Flyway** 自动创建（`backend/src/main/resources/db/migration`），
连接串带 `createDatabaseIfNotExist=true`，所以**只要 MySQL 在跑，库和表都会自动就绪**。

只有想灌演示数据时才需要执行脚本（10 个分类 / 45 本演示图书 / 11 个用户 / 15 条借阅 / 11 条收藏，
按 ISBN 先删后建，可重复执行）：

```bash
cd library-system
mysql -uroot -p123456 library_system_db < sql/seed-demo-data.sql
```

> 如果你的 root 密码不是 `123456`：启动后端时设置环境变量 `MYSQL_PASSWORD`，
> 或直接改 `backend/src/main/resources/application.yml` 里的默认值。

**② 启动后端**（新开一个终端）

```bash
cd library-system/backend
mvn spring-boot:run
```

看到 `Started LibraryApplication` 即成功，服务在 http://localhost:8080 。
启动日志里会打印 Flyway 的迁移记录（`Migrating schema ... to version "1 - baseline schema"`），
首次启动同时会创建默认账号。

**③ 启动前端**（再开一个终端）

```bash
cd library-system/frontend
npm install        # 首次需要，约 1~5 分钟（视网络）
npm run dev
```

浏览器打开 **http://localhost:5173** 即可。

> 两个终端要一直开着：关掉后端，页面就会提示「无法连接后端服务，请确认后端已在 8080 端口启动」。
> 想变成一条命令启动，可自行加一个脚本，内容是并排拉起上面两条命令（`start /b` 或 `&`），不需要额外依赖。

### 默认账号（后端首次启动自动创建）

| 账号 | 密码 | 角色 | 可见菜单 |
|---|---|---|---|
| admin | admin123 | 管理员 | 数据统计、图书检索、图书管理、分类管理、借阅管理、用户管理、公告、个人中心 |
| reader | 123456 | 读者 | 图书检索、系统公告、个人中心 |
| reader2 | 123456 | 读者 | 同上 |

---

## 二、方式二：Docker 一键启动（可选）

若已安装 Docker，可完全用容器运行（MySQL + 后端 + 前端三容器）：

```bash
cd library-system
cp .env.example .env          # 可选：修改密码 / JWT 密钥 / 跨域来源
docker compose up -d --build
```

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost |
| 后端 | http://localhost:8080/api |
| 接口文档 | http://localhost:8080/swagger-ui.html |
| 健康检查 | http://localhost:8080/actuator/health |
| MySQL | **localhost:13306**（root / root123，库 `library_system_db`） |

> - MySQL 映射到宿主机 **13306**，避免与本机已装的 MySQL（3306）冲突；容器内仍是 3306，后端通过 `mysql:3306` 访问。
> - 三个服务都配了 `healthcheck`：`frontend` 等 `backend` 健康后才启动，`backend` 等 `mysql` 健康后才启动，不会出现「容器起来了但连不上库」。
> - 前端镜像构建为 Nginx 静态托管 + `/api` 反向代理到 `backend:8080`，与本地开发行为一致。
> - 后端容器以非 root 用户（uid 1001）运行，并通过 `JAVA_OPTS` 限制堆占宿主内存的 75%。
> - `8080` 与 `13306` 只绑定 `127.0.0.1`，公网访问不到；需要在浏览器看 Swagger 时用 SSH 隧道
>   （`ssh -L 8080:127.0.0.1:8080 user@server`）。
> - 上面的密码/端口以 `.env` 为准，本表格只是 `.env.example` 的默认值。
>
> **要部署到公网服务器（域名、HTTPS、备份、排错）请看 [`DEPLOY.md`](./DEPLOY.md)**，里面有两套完整方案：
> Docker Compose 一键部署，以及 Nginx + systemd + 本机 MySQL 的手工部署。

---

## 三、目录结构

```
library-system/
├── DEPLOY.md                     # 部署手册：Docker Compose / Nginx + systemd 两套方案、备份、排错
├── docker-compose.yml            # MySQL + 后端 + 前端编排
├── .env.example                  # compose 的环境变量模板（复制为 .env 后改密码与密钥）
├── .gitignore                    # 排除 target/node_modules/.idea/日志等
├── .editorconfig / .gitattributes  # 编码、缩进、换行统一
├── scripts/hooks/commit-msg      # 依赖零安装的 commit-msg 钩子（核心逻辑，可版本化）
├── sql/seed-demo-data.sql        # 演示数据（10 分类 / 45 图书 / 11 用户 / 15 借阅），可重复执行
├── scripts/verify-api.mjs        # 接口自检脚本（node scripts/verify-api.mjs）
├── scripts/verify-crud.mjs       # 写接口回归脚本（图书/分类/公告的增删改与校验，含数据清理）
├── scripts/make-release.sh       # 打「仅源码」发布包（排除 node_modules/target/dist）
├── backend/
│   ├── pom.xml / Dockerfile / .dockerignore
│   ├── uploads/                    # 运行期上传的封面（已 gitignore，容器里挂卷）
│   └── src/
│       ├── main/resources/db/migration/      # Flyway 迁移脚本：唯一的表结构来源
│       │   ├── V1__baseline_schema.sql       # 基线：7 张表（幂等，兼容旧库）
│       │   ├── V2__reference_data.sql        # 参考数据：10 个分类 + 欢迎公告
│       │   └── V3__reservation_and_notification.sql  # 预约表 + 站内信表 + book.held 列
│       ├── main/java/com/library/
│       │   ├── LibraryApplication.java
│       │   ├── common/                       # 通用能力，不含任何业务语义
│       │   │   ├── config/                   # MybatisPlus / Jackson / OpenApi / SecurityConfig / DataInitializer
│       │   │   ├── constant/RoleConstants    # 角色与默认密码常量，禁止在业务代码里写字面量
│       │   │   ├── log/Audit.java            # @Audit 审计注解（切面实现放在 module.log）
│       │   │   ├── log/TraceContext.java     # traceId 的 MDC 上下文（生成 / 校验 / 标记用户 / 清理）
│       │   │   ├── log/TraceIdFilter.java    # 最外层过滤器：traceId + 访问日志（按状态码分级）
│       │   │   ├── exception/                # BizException + GlobalExceptionHandler
│       │   │   ├── result/Result.java        # 统一响应 {code,message,data,timestamp}
│       │   │   ├── security/                 # JwtUtil / JwtAuthenticationFilter / LoginUser / SecurityUtil
│       │   │   ├── util/CsvUtil.java         # CSV 导出与解析（UTF-8 BOM + 字段转义 + 引号状态机）
│       │   │   ├── util/ExcelUtil.java       # xlsx 导出（EasyExcel 无实体模型写法，自适应列宽）
│       │   │   ├── util/IpUtil.java          # 真实客户端 IP（X-Forwarded-For 优先），审计与限流共用
│       │   │   └── web/RootController.java   # 根路径探针，避免把 8080 当页面地址访问
│       │   └── module/                       # 业务模块：统一 controller / dto / entity / mapper / service(+impl)
│       │       ├── auth/       # 登录 / 注册 / 当前用户（AuthService）
│       │       ├── user/       # 用户档案 CRUD（UserService）
│       │       ├── category/   # 分类
│       │       ├── book/       # 图书（BookMapper 内含条件扣减库存 SQL）
│       │       ├── borrow/     # 借阅事务（借书 / 还书 / 续借）+ task/ 逾期扫描、到期前提醒
│       │       │               # BorrowQueryService：给预约模块用的只读窄接口（断开循环依赖）
│       │       ├── notice/     # 公告
│       │       ├── favorite/   # 收藏
│       │       ├── portal/     # 读者门户聚合（PortalService + PortalOverviewVO）
│       │       ├── stat/       # 后台统计（概览 + 趋势 + 分类分布 + 借阅排行）
│       │       ├── reservation/ # 预约排队（reserve / cancel / handOff 让位 / sweepExpired 过期清扫）
│       │       ├── notification/ # 站内信（到书通知 / 到期提醒，dedupe_key 唯一索引做幂等）
│       │       ├── file/       # 文件上传（封面落本地磁盘，FileStorageService 可替换为 OSS 实现）
│       │       └── log/        # 操作日志（Audit 切面 + 查询接口）
│       ├── main/resources/application.yml
│       ├── main/resources/logback-spring.xml # 日志双形态：dev 彩色文本 / prod 单行 JSON + 滚动文件
│       └── test/java/com/library/            # 单元测试（JWT、CSV、traceId 链路、接口契约）
└── frontend/
    ├── package.json / vite.config.js / index.html / Dockerfile / nginx.conf
    ├── eslint.config.mjs / .prettierrc.json           # 静态检查与格式化配置
    ├── public/covers/                # 图书封面图（按 ISBN 命名，53 本已全量配图，单张 100–190KB）
    └── src/
        ├── main.js / App.vue
        ├── styles/theme.css              # 设计令牌：暖米色 / 深绿 / 金色，全局样式
        ├── router/index.js               # 路由 + 守卫（登录态、角色）
        ├── stores/user.js                # Pinia：token / 用户信息
        ├── utils/request.js              # Axios 拦截器（携带 JWT、统一响应、401 跳转）
        ├── utils/index.js                # 借阅状态与逾期判断
        ├── utils/echarts.js              # ECharts 按需注册 + 统一配色
        ├── components/EChart.vue         # 图表封装（挂载 / 更新 / 尺寸自适应 / 销毁）
        ├── components/NotificationBell.vue  # 铃铛：未读数轮询 + 下拉通知列表（两种布局共用）
        ├── api/                          # auth/book/category/user/borrow/notice/favorite/stat/portal
        │                                 # + reservation / notification / file
        └── views/                        # Login / Layout / Dashboard / BookList / BookManage /
                                          # CategoryManage / UserManage / BorrowManage / ReservationManage /
                                          # NoticeList / OperationLog / Profile
```

### 分层约定

每个业务模块内部固定这几层，缺一层通常说明职责没拆干净：

| 层 | 职责 | 禁止 |
|---|---|---|
| `controller` | 参数绑定、`@Valid` 校验、调 service、包装 `Result` | 写业务判断、直接调 mapper |
| `dto` | 入参 `XxxDTO`、出参 `XxxVO` | 拿 entity 当请求体（前端可能改到 `deleted` / `borrowed` / `stock`） |
| `entity` | 与表结构一一对应 | 出现业务方法 |
| `mapper` | 单表查询、自定义更新 SQL | 写业务流程 |
| `service` + `service/impl` | 业务规则、事务边界、跨模块编排 | 依赖 `HttpServletRequest` 等 Web 对象 |

调用方向固定为「controller → 本模块 service → 其他模块 service」，不跨模块直接调 mapper；响应体、异常、安全、常量等通用能力一律放 `common`。

---

## 四、已验证项（本机实测结果）

**接口自检**（`node scripts/verify-api.mjs`，后端启动后执行）：

```
PASS | 管理员登录 / 读者登录 / 错误密码被拒绝
PASS | 图书分页检索（关键字 + 分类过滤）
PASS | 借书成功 → 库存 5→4；已借出 0→1
PASS | 借阅记录生成（应还时间 = 借出 + 30 天）
PASS | 续借成功 → 应还日期顺延 30 天、续借次数=1；超次数续借被拒绝
PASS | 还书成功 → 库存回补 4→5；记录状态=已还且写入归还时间
PASS | 读者访问用户管理/全量借阅记录 → 403
PASS | 未登录访问 → 401
PASS | 收藏成功、重复收藏被拒绝
PASS | 管理员统计接口、注册参数校验
```

**写接口回归**（`node scripts/verify-crud.mjs`，17 项，自动清理测试数据）：

```
PASS | 新增图书 → 检索命中、borrowed 由服务端置 0、状态默认上架
PASS | 编辑图书 → 前端传入的 stock 被忽略（仍为原值）
PASS | 超长 ISBN / ISBN 重复 / 非法上下架状态 → 均返回 400 而非 500
PASS | 分类新增、重名拦截、列表可见
PASS | 公告发布、查询、编辑、空标题被 @Valid 拦截
```

**前端**：`npm run build` 通过（10 个页面全部编译）；无头浏览器实测登录页、图书检索页（含 8 条图书数据与分页）、个人中心（借阅记录/收藏）渲染正常，浏览器控制台无报错。

**日志与追踪**：dev 形态下 200 / 400 / 401 三类请求分别打印带 traceId 的访问日志
（`GET /api/books/page -> 200 (151ms)`）；上游传 `X-Trace-Id: gateway-abc-12345` 原样回传，
传 `abc def` 这类非法值被替换为新生成的 32 位 id；`prod` 形态输出单行 JSON，
含 `traceId / userId / username / httpStatus / costMs / clientIp`，stdout 与落盘文件内容一致，无本机路径泄漏。

**预约排队 + 到书通知**（用《艺术的故事》走完整链路，实测后数据已还原）：

```
初始           stock=1 borrowed=1 held=0
管理员借出     stock=0 borrowed=2 held=0
读者预约       预约#1 排队中，/book/288 → waitingCount=1 myPosition=1
重复预约       400「你已预约过这本书」                ← uk_active 唯一索引拦住
管理员归还     stock=0 borrowed=1 held=1            ← 副本从 stock 挪进 held，别人抢不走
队首状态       myStatus=1（待取书），保留至 11-23（7 天）
站内信         1 RESERVATION_READY「预约到书：《艺术的故事》」，未读数 1
读者借书       stock=0 borrowed=2 held=0，预约 → status=4（已借出）  ← 走 lendHeld 消耗锁定副本
读者归还       stock=1 borrowed=1 held=0            ← 库存口径完全复原
```

**到期前提醒**：`POST /api/borrows/due-soon-remind` 三层验证——
默认 3 天窗口返回 `sent=0`（在借记录都在 30 天后到期）；放宽到 30 天返回 `sent=7`（7 条在借各一条站内信）；
同参数再跑一次 `sent=0`，证明 `dedupe_key` 幂等生效，不会每天重复骚扰同一个读者。

---

## 五、核心业务规则与实现要点

- **借阅规则**（`application.yml` → `library.borrow` 可调）：
  `max-count: 5`（同时在借上限）、`days: 30`（借期）、`max-renew: 1`（续借次数）
- **借书事务**（`BorrowServiceImpl#borrowBook`，`@Transactional(rollbackFor = Exception.class)` 单事务）：
  1. 校验账户状态、在借数量、是否存在逾期未还（逾期按 `due_date < now` 判断）；
  2. 条件更新库存：
     ```sql
     UPDATE book SET stock = stock - 1, borrowed = borrowed + 1
     WHERE id = ? AND stock > 0 AND status = 1 AND deleted = 0
     ```
     受影响行数为 0 即抛 `BizException` 触发回滚，杜绝并发超借；
  3. 写入 `borrow_record`（`borrow_date = now`、`due_date = now + 30 天`、`status = 0`）。
- **还书**：同一事务内 `returnStock`（stock+1 / borrowed-1）并更新 `status=1`、`return_date`；记录永久保留，作为统计与审计基础。
- **软删除**：`user`、`book` 通过 `deleted` 字段 + MyBatis-Plus `@TableLogic` 逻辑删除，借阅历史不受影响。
- **权限**：JWT 无状态认证，方法级 `@PreAuthorize("hasRole('ADMIN')")` / `hasAnyRole('ADMIN','USER')`；
  前端路由 `meta.roles` 做二次拦截（后端为最终防线）。
- **密码**：BCrypt 加密存储；`User.password` 使用 `@JsonProperty(WRITE_ONLY)`，永不出现在响应中。
- **统一响应**：`{ code, message, data, timestamp }`；401 未登录、403 无权限、400 业务/参数错误、500 系统异常。
- **逾期扫描**（`module.borrow.task.OverdueCheckTask`，`@Scheduled` 每天 01:00）：
  把「在借且 `due_date < now`」的记录刷成 `overdue = 1`，续借后未到期则自愈回 0；
  存在逾期时发布当日催还公告（同一天只发一条，幂等）。管理员也可 `POST /api/borrows/overdue-check` 手动触发。
  标记只是**派生状态**，借书时的逾期判断仍实时看 `due_date`，因此任务漏跑不会误放行。
- **操作审计**（`@Audit` + `OperationLogAspect`）：切在 Controller 层，能拿到 URl / IP / HTTP 方法；
  先执行原方法再写日志，写入失败只告警不影响业务；`password` 之类字段自动打码，入参超 1000 字符截断。
- **日志与追踪**（`TraceContext` + `TraceIdFilter`）：每个请求生成 32 位 traceId 写入 MDC，
  响应头 `X-Trace-Id` 回传，同一次调用打出的所有日志（含鉴权、业务、SQL 耗时、异常堆栈）都带同一个值；
  `userId/username/uri/httpStatus/costMs` 一并进 MDC，生产环境输出单行 JSON，详见「九.11 结构化日志与 traceId」。
- **预约排队**（`module.reservation`）：库存口径固定为 `stock（可借）+ held（待取书锁定）+ borrowed（已借出）= 馆藏总量`。
  读者借书只认 `stock`，所以被预约锁定的副本不会被别人抢走。三个不变量：
  1. 一人一书同时只有一条进行中的预约 —— 靠唯一索引 `uk_active(user_id, book_id, active_flag)`，
     终态（取消/过期/已借出）把 `active_flag` 置 NULL 从而允许历史累积；
  2. `book.held` 恒等于「未过期的待取书预约数」——`holdCopy` / `releaseHold` / `lendHeld` 全是带条件的 UPDATE，
     受影响行数为 0 就放弃，不做「先查再改」；
  3. 队列顺序只看自增 ID，不看时间戳 —— 同一秒多人预约时时间戳分不出先后。
  安全阀：可借库存 > 0 时不许预约、有逾期不许预约、最多同时 3 个预约（`library.reservation` 可调）。
  到馆保留期默认 7 天，`ReservationSweepTask` 每小时扫一次（不是每天，热门书每一小时都值钱），
  过期未取则副本自动顺延给下一位并发「预约已过期」通知。
- **站内信**（`module.notification`）：`dict` 化的 `type` 字符串（DUE_SOON / RESERVATION_READY / RESERVATION_EXPIRED），
  `dedupe_key` 唯一索引承担幂等——定时任务重跑、集群两台机器同时跑都不会重复发信。
  刻意不加 `@Transactional`：单条 insert 本身原子，若挂在调用方事务里，幂等冲突会让整个业务事务被标记
  rollback-only（Spring 的坑），冲突时只影响这一次投递。
- **到期前提醒**（`DueReminderTask` 每天 09:00 + `POST /api/borrows/due-soon-remind` 手动补跑）：
  窗口 `now <= due_date <= now + due-soon-days(3)`，两端都闭；已逾期的**不在**窗口内（那是催还公告的职责）。
  幂等键 `DUE_SOON:借阅ID:应还日期` 带日期，读者续借后应还日期变了会再提醒一次，同一日期绝不重复。
  窗口判定抽在 `DueSoonPolicy`（纯函数），边界由此可以被单测钉死。
- **借阅记录导出（CSV / Excel）**：同一份表头与数据，按 `format` 参数二选一——`CsvUtil` 零依赖实现
  （UTF-8 BOM 保证 Excel 打开不乱码，含逗号/引号的书名正确转义）；`format=xlsx` 走 `ExcelUtil`（EasyExcel
  无实体模型写法，锁 3.3.4 因 3.4+ 要 Java 21），自适应列宽、数字列是真 Number 可直接求和、空数据也产出合法文件。
- **图书封面**：`book.cover_url` 存封面地址（站内相对路径 `/covers/xxx.jpg` 或完整 URL）。
  有值 → 门户卡片、详情弹窗、收藏列表、后台表格统一展示真实封面；为空或图片 404 → 自动降级为「配色书封」
  （按 `id % 5` 取稳定配色 + 书名 + ISBN），因此没有封面的书也不会出现破图。封面图放在 `frontend/public/covers/`，
  按 ISBN 命名便于人工维护；生产环境构建后由 nginx 直接托管静态文件。
  `book.cover_url` 列由 Flyway 的 `V1__baseline_schema.sql` 建立，新增环境无需再手工 ALTER。
- **封面后台上传**：`POST /api/files/cover`（ADMIN）把图片落到 `library.upload.dir`（默认 `backend/uploads`）下的
  `covers/yyyyMMdd/`，文件名用 UUID 生成，再通过 `WebMvcConfig` 映射到 `/uploads/**` 对外提供访问。
  三道校验缺一不可：扩展名白名单（jpg/png/webp，挡掉 .svg/.jsp）、大小上限 2MB、**文件头魔数**（防止把任意文件
  改名成 .jpg 上传）。前端图书表单里嵌了上传按钮，传完自动回填「封面地址」，也可以继续手填外部 URL。

---

## 六、API 一览

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 匿名 | 登录，返回 JWT |
| POST | /api/auth/register | 匿名 | 注册（读者角色） |
| GET | /api/auth/me | 登录 | 当前用户信息 |
| POST | /api/auth/password | 登录 | 自助修改密码（校验原密码，6-32 位） |
| GET | /api/users/page | ADMIN | 用户分页（用户名模糊） |
| POST / PUT / DELETE | /api/users/** | ADMIN | 新增 / 编辑 / 启禁用 / 重置密码 / 软删除 |
| GET | /api/books/page | 登录 | 图书分页检索（keyword / categoryId / status） |
| GET | /api/books/{id} | 登录 | 图书详情 |
| POST / PUT / DELETE | /api/books/** | ADMIN | 图书新增 / 编辑 / 上下架 / 软删除 |
| POST | /api/books/import | ADMIN | 批量导入 CSV（逐行校验，返回成功/跳过/失败明细） |
| GET | /api/books/import-template | ADMIN | 下载导入模板（表头 + 两行示例，UTF-8 BOM） |
| POST | /api/files/cover | ADMIN | 上传图书封面，返回 `/uploads/covers/...` 相对地址 |
| GET | /uploads/** | 公开 | 已上传封面的静态访问（匿名可读，img 标签才能直接加载） |
| GET | /api/categories | 登录 | 分类列表 |
| POST / PUT / DELETE | /api/categories/** | ADMIN | 分类管理 |
| POST | /api/borrows?bookId= | 登录 | 借书 |
| PUT | /api/borrows/{id}/return | 登录 | 还书（本人或管理员） |
| PUT | /api/borrows/{id}/renew | 登录 | 续借（本人或管理员） |
| GET | /api/borrows/my | 登录 | 我的借阅历史（分页） |
| GET | /api/borrows/page | ADMIN | 全部借阅记录（分页） |
| POST | /api/borrows/overdue-check | ADMIN | 手动触发逾期扫描（刷新标记 + 发布催还公告，幂等） |
| POST | /api/borrows/due-soon-remind | ADMIN | 手动触发到期前提醒（幂等，可不传 aheadDays 用配置值） |
| GET | /api/borrows/export | ADMIN | 导出借阅记录（`format=xlsx` 为 Excel，默认 CSV 带 BOM） |
| POST | /api/reservations?bookId= | 登录 | 预约一本已借完的书（有库存 / 有逾期 / 已在借时拒绝） |
| DELETE | /api/reservations/{id} | 登录 | 取消预约（本人或管理员；待取书会顺延给下一位） |
| GET | /api/reservations/my | 登录 | 我的预约（含排队位置、保留截止时间） |
| GET | /api/reservations/book/{bookId} | 登录 | 单书预约态：队列长度 + 我的状态（详情页按钮用） |
| GET | /api/reservations/page | ADMIN | 预约队列分页（状态 / 书名筛选） |
| GET | /api/notifications/page | 登录 | 我的站内信分页（unreadOnly=true 只看未读） |
| GET | /api/notifications/unread-count | 登录 | 未读数（铃铛角标轮询） |
| PUT | /api/notifications/{id}/read | 登录 | 标记单条已读 |
| PUT | /api/notifications/read-all | 登录 | 全部标为已读 |
| GET | /api/notices/page | 登录 | 公告分页 |
| POST / PUT / DELETE | /api/notices/** | ADMIN | 公告管理 |
| POST / DELETE | /api/favorites/{bookId} | 登录 | 收藏 / 取消收藏 |
| GET | /api/favorites/my | 登录 | 我的收藏 |
| GET | /api/stat/overview | ADMIN | 统计概览（6 项指标卡） |
| GET | /api/stat/borrow-trend?days=30 | ADMIN | 近 N 天借还趋势（缺失日期补 0） |
| GET | /api/stat/category-distribution | ADMIN | 分类馆藏分布（含零馆藏分类） |
| GET | /api/stat/top-books?limit=10 | ADMIN | 借阅次数排行 |
| GET | /api/logs/page | ADMIN | 操作日志分页（模块 / 结果 / 关键字筛选） |

---

## 七、常见问题（FAQ）

### 1. 打开 http://localhost:8080 看到「HTTP状态 404 - 未找到 / Apache Tomcat/9.0.123」

**这不是本项目**。本项目后端是 Spring Boot 内嵌 **Tomcat 10**，访问根路径会返回一段 JSON 说明
（`{"code":200,"data":{"应用":"图书管理系统 API",...}}`）。如果看到 Tomcat 9 的 404 页面，说明 8080
被别的容器占了（最常见是 **IDEA 内置 Tomcat**，或本机 D:\apache-tomcat-9.x）。

判断方法：

```bash
netstat -ano | findstr ":8080" | findstr LISTENING   # 拿到 PID
tasklist /FI "PID eq <PID>"                          # 看是 java.exe 还是别的
```

- 占用者是 IDEA 的 Tomcat：IDEA → Services 面板 → 选中 Tomcat → 红色停止按钮；
  或命令行 `taskkill /F /PID <PID>`（`taskkill` 只杀进程，不改 IDEA 配置，随时可再启动）。
- 想保留占用、让本项目换端口：`set SERVER_PORT=8081` 启动后端，
  前端用 `set VITE_PROXY_TARGET=http://localhost:8081 && npm run dev` 启动即可，代码无需改动。

### 2. 页面能打开，但登录提示「用户名或密码错误」

按顺序排查：

1. 确认后端在跑：浏览器开 http://localhost:8080/ ，应看到 JSON 说明而不是 404/401；
2. 确认账号源于本库：`mysql -uroot -p123456 -e "USE library_system_db; SELECT id,username,role,status FROM user;"`
   应看到 admin/reader/reader2 三行；
3. 留意输入法：**全角 `ａｄｍｉｎ` 和半角 `admin` 是不同的字符串**，密码同理；
4. 后端日志会打印 `登录失败 username=[xxx] 账号存在=true/false`，可直接区分是「账号不存在」还是「密码不对」。

### 3. 其它

1. **端口被占用**：前端默认 5173、后端 8080、MySQL 3306。改端口：
   - 后端：`SERVER_PORT=8081`（环境变量）或 `application.yml` → `server.port`
   - 前端：`npm run dev -- --port 5174`，同时用 `VITE_PROXY_TARGET` 指向实际后端地址
2. **数据库连不上**：确认 MySQL 服务已启动、`application.yml` 中账号密码正确；可用
   `mysql -uroot -p你的密码 -e "USE library_system_db; SHOW TABLES;"` 验证。
3. **重建数据**：`mysql -uroot -p你的密码 -e "DROP DATABASE library_system_db;"`，然后重启后端——
   Flyway 会在空库上自动重建全部表（历史表也会重建），再按需执行 `sql/seed-demo-data.sql`。
4. **Docker 重建**：`docker compose down -v`（`-v` 删除数据卷）后再 `up`。
5. **npm install 慢**：项目默认走 `registry.npmmirror.com`；如仍慢可执行
   `npm config set registry https://registry.npmmirror.com` 后重试。
6. **登录 401 / Token 过期**：JWT 默认 2 小时过期，重新登录即可；生产环境必须通过环境变量
   `JWT_SECRET` 覆盖默认密钥（长度 ≥ 32 字符），并修改 MySQL root 密码。
7. **生产环境加固建议**：关闭 `application.yml` 中 MyBatis SQL 日志（`log-impl`）、
   收敛 `SecurityConfig` 的 CORS 允许来源、为 `library_system_db` 建独立只读写账号而非用 root。

---

## 八、界面设计说明（读者门户 + 管理后台双形态）

系统按角色自动切换两套界面，共用同一套设计令牌（`frontend/src/styles/theme.css`）：

| 令牌 | 值 | 用途 |
|---|---|---|
| `--cream` | `#faf7f0` | 全站底色（暖米色） |
| `--forest` | `#1f4b3a` | 品牌主色（深绿）：侧边栏、搜索区、按钮 |
| `--gold` | `#d9a441` | 强调色（金色）：主操作按钮、角色徽章 |
| `--ink` / `--ink-3` | `#1c2b24` / `#8a8778` | 主文字 / 辅助文字 |
| `--radius` | `14px` | 卡片圆角 |

**读者端（门户形态）**——登录后直接进入 `/books`，顶部横向导航（馆藏检索 / 我的借阅 / 系统公告）：

- **馆藏检索**：深绿大搜索区 + 分类下拉 + 关键词输入 + 金色「检索」按钮，下方三个实时统计药丸（馆藏 N 种、可借 N 册、读者 N 人）。
- **图书分类**：四列卡片，点击即按分类过滤。
- **新书速递**：书封网格。书封用书名哈希生成稳定配色（同一本书每次颜色一致），封面上显示书名与 ISBN；卡片下方是书名、`作者 · 分类`、`可借 N/M`、详情按钮。
- **图书详情弹窗**：左侧书封、右侧书名/作者/两列元信息（ISBN、分类、出版社、库存、可借、排队人数）、简介；
  底部按钮随状态切换：「借阅」（有库存，或已有锁定给他的待取副本）→「预约排队（前方 N 人）」（已借完）
  →「排队中 · 第 N 位 + 取消预约」（已排队）→「已到馆，保留至 x 月 x 日」（待取书）。
- **通知铃铛**：顶栏常驻，未读数角标 + 下拉最近 8 条（到期提醒金色 / 到书绿点 / 过期红点），60 秒轮询；点条目直接跳到对应标签页。
- **我的借阅 / 系统公告 / 个人中心**：同色系卡片式排版，借阅记录带状态标签与归还、续借操作。

**管理端（后台形态）**——管理员登录后进入 `/dashboard`，左侧深绿侧边栏（10 个菜单，按角色过滤）+ 浅色内容区：

- **数据统计**：六个指标卡（馆藏种类、藏书总量、注册读者、在借册数、逾期未还、累计借阅），数字用无衬线粗体，单位用小号上标。
- **图书 / 分类 / 用户 / 借阅 / 预约 / 公告管理**：统一「查询区卡片 + 表格卡片」结构；操作列按钮并排不换行，分类页用文字按钮减重。
- **预约队列**：顶部一条说明横幅（保留 N 天、超时自动顺延的规则），表格带「进度」列——排队中显示 `排第 N 位`，
  待取书显示 `保留至 x 月 x 日`，管理员可直接代读者取消。

**为什么这样分**：读者是「逛」的心态，需要大图、书封、搜索前置；管理员是「办事」的心态，需要表格密度、批量操作、左侧常驻导航。两套形态共用令牌，视觉上是一家人，交互上各走各的最优解。

---

## 九、工程规范（代码质量 / 测试 / 文档 / 打包）

### 1. 编码与换行：`.editorconfig` + `.gitattributes`

两个文件解决「换个人一提交就是整文件 diff」的问题：

| 规则 | 值 | 原因 |
|---|---|---|
| 文件编码 | UTF-8，无 BOM | 中文不乱码；BOM 会让 shell 脚本首行失效 |
| 换行 | LF（`.bat` 强制 CRLF） | Windows 下 cmd 对 LF-only 批处理解析异常 |
| 缩进 | Java/XML 4 空格；JS/Vue/CSS/YAML 2 空格 | 与现有代码一致 |
| 行尾空格 | 自动清理（`.md` 除外） | 避免无意义 diff |

### 2. 前端静态检查：ESLint + Prettier

```bash
cd frontend
npm run lint          # 检查（CI 用这个）
npm run lint:fix      # 自动修可修项
npm run format        # Prettier 统一格式
npm run format:check  # 只检查不改动（CI 用这个）
```

- ESLint 采用**扁平配置**（`eslint.config.mjs`，ESLint 9），规则集用 `flat/essential`——只抓「会出错」的问题（未定义变量、重复 key、模板误用、`script setup` 之外的写法），不掺风格规则；
- 所有格式统一交给 Prettier（`.prettierrc.json`：无分号、单引号、100 列、LF）；
- 当前存量代码 **ESLint 0 error、Prettier 全部通过**，可以直接接入 CI 卡口。

### 3. 后端测试：JUnit 5 + Mockito + MockMvc

```bash
cd backend
mvn test                            # 61 个测试：58 个单测通过 + 3 个集成用例（无 Docker 时自动跳过）
mvn test -Dtest=JwtUtilTest         # 只跑某个类
```

| 测试类 | 覆盖内容 |
|---|---|
| `JwtUtilTest` | token 生成/解析、载荷不丢失、篡改/过期/换密钥/格式非法均返回 null |
| `AuthControllerTest` | 登录成功返回统一结构、空用户名被校验拦截、密码错误转换业务异常、注册用户名过短被拦截、未登录访问 `/api/auth/me` 返回 401、改密参数校验、锁定返回 429 |
| `LoginAttemptServiceTest` | 连续失败 5 次锁定、剩余次数递减、成功登录清零、同 IP 超阈值限流、失败计数按账号累计 |
| `CsvUtilTest` | CSV 解析边界：引号内逗号/换行、`""` 转义、BOM 剥离、CRLF/CR/LF 混用、空行跳过、`escape` 与 `parse` 互逆 |
| `ExcelUtilTest` | EasyExcel 写出后用 POI 读回：表头/数据逐格一致、中文不乱码、数字列是 Number 类型、空数据仍产出合法文件、非法工作表名被替换且 ≤31 字符、1000 行流式写出 |
| `TraceIdFilterTest` | traceId 自动生成、上游透传、非法值（含换行注入）被替换、访问日志 MDC 字段、500 文案带追踪号、请求后 MDC 清空、静默路径不刷日志 |
| `DueSoonPolicyTest` | 提醒窗口两端边界（今天到期 / 第 N 天 / 第 N+1 天 / 晚 1 秒）、已逾期不重复提醒、应还时间为空、幂等键随续借变化 |
| `ReservationServiceImplTest` | 让位三条不变量：无可锁定副本时放弃、队首被抢占时归还副本（防 `held` 泄漏）、正常让位发通知且幂等键按预约 ID 生成；取消「排队中」不碰库存、取消「待取书」释放副本、读者不能取消他人预约 |
| `BorrowTransactionIntegrationTest` | **Testcontainers + 真实 MySQL**：借书扣库存并生成记录、库存为 0 时失败且不留脏数据、库存 1 时两人并发只能成功 1 人（不超借） |

前几个类**不依赖 Spring 容器和数据库**（standalone MockMvc + ReflectionTestUtils），秒级跑完，作为「接口格式被改坏」的护栏。
集成测试用 `@Testcontainers(disabledWithoutDocker = true)`：本机没装 Docker 时会整体跳过（显示 `Skipped: 3`），
CI（ubuntu runner 自带 Docker）里会真正拉起 `mysql:8.0.33`，表结构由应用启动时的 **Flyway 迁移**创建，
测试跑的就是线上那套 `V1/V2/V3` 脚本，不存在「测试 DDL 与生产 DDL 两份」的漂移。
如果本机装了 Docker 想强制跑：`mvn test -Dtest=BorrowTransactionIntegrationTest`。

### 4. 接口文档：springdoc-openapi

启动后访问 <http://localhost:8080/swagger-ui.html>，点右上角 **Authorize** 粘贴登录返回的 token（不用加 `Bearer ` 前缀）即可调试所有受保护接口。
在线 JSON：<http://localhost:8080/v3/api-docs>。生产环境如需关闭，把 `springdoc.api-docs.enabled` 设为 `false`。

### 5. 健康检查与指标：actuator

```bash
curl http://localhost:8080/actuator/health    # {"status":"UP","groups":["liveness","readiness"]}
curl http://localhost:8080/actuator/metrics   # 需要 token（已配 when-authorized）
```

`/actuator/health` 已放行，供 Docker healthcheck、Nginx upstream 检查、K8s 探针使用。

### 6. 统一异常与 HTTP 状态码

`GlobalExceptionHandler` 保证响应体永远是 `{code, message, data, timestamp}`，同时**让 HTTP 状态码与业务 code 对齐**：

| 场景 | HTTP | body.code |
|---|---|---|
| 业务校验失败（如库存不足） | 400 | 400 |
| 参数校验失败（`@Valid`） | 400 | 400 |
| 权限不足（`@PreAuthorize`） | 403 | 403 |
| 未登录 / token 过期 | 401 | 401 |
| 未捕获异常 | 500 | 500 |

> 早期版本一律返回 HTTP 200 + body.code，导致 Nginx 访问日志、网关告警、浏览器 DevTools 都看不出错误。现已修正，前端 `request.js` 从响应体读 message，行为不受影响。

### 7. 配置与密钥：`.env.example`

所有可变配置都走环境变量，不写死在代码里：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | 8080 | 后端端口 |
| `MYSQL_HOST/PORT/USERNAME/PASSWORD` | localhost/3306/root/123456 | 数据库连接 |
| `JWT_SECRET` | 开发用固定值 | **生产必须覆盖**，≥32 字符，`openssl rand -base64 48` |
| `CORS_ORIGINS` | localhost:5173 等 | 允许跨域的来源，逗号分隔，**不要写 `*`** |
| `MYBATIS_LOG_IMPL` | StdOutImpl | 生产设 `...nologging.NoLoggingImpl` 关闭 SQL 日志 |
| `SPRING_PROFILES_ACTIVE` | （空，即 dev 形态） | 设 `prod` 输出单行 JSON 日志（`docker-compose` 里已默认 prod） |
| `LOG_LEVEL` | info | 业务包日志级别，排障时临时设 `debug`，无需重新打包 |
| `LOG_DIR` | ./logs | prod 下 JSON 日志落盘目录；容器内为 `/app/logs`（挂命名卷） |
| `OVERDUE_CRON` | `0 0 1 * * ?` | 逾期扫描时间，演示时可改成每 10 分钟 |
| `DUE_SOON_DAYS` | 3 | 到期前提醒窗口（天） |
| `DUE_SOON_CRON` | `0 0 9 * * ?` | 到期前提醒执行时间 |
| `RESERVATION_HOLD_DAYS` | 7 | 预约到书后的保留天数，超时自动顺延 |
| `RESERVATION_MAX_COUNT` | 3 | 单个读者同时进行中的预约上限 |
| `RESERVATION_SWEEP_CRON` | `0 5 * * * ?` | 预约过期清扫频率（默认每小时） |
| `VITE_PROXY_TARGET` | http://localhost:8080 | 前端 dev 代理目标 |

### 8. CI：GitHub Actions

`.github/workflows/ci.yml`：PR / push 到 `main`、`develop` 时自动执行
`mvn test` → `mvn package` → `npm ci` → `npm run lint` → `npm run format:check` → `npm run build`，并上传 jar 与 dist 产物。

### 9. 交付打包：别把 node_modules 打进去

```bash
bash scripts/make-release.sh        # 生成 library-system-src.tgz（仅源码）
bash scripts/make-release.sh zip    # 生成 zip
```

自动排除 `node_modules` / `target` / `dist` / `.idea` / `logs` / `*.log` / `.env`。
实测：整个目录压缩约 **54MB**，源码包仅 **97KB**。若项目已 `git init` 并提交，脚本会自动改用 `git archive`，只打已跟踪文件，最干净。

### 10. 数据库迁移：Flyway

表结构只有一个来源——`backend/src/main/resources/db/migration`。启动时自动执行，
执行记录写入 `flyway_schema_history`，谁在哪个环境跑了哪些脚本一目了然。

| 脚本 | 作用 |
|---|---|
| `V1__baseline_schema.sql` | 7 张表的基线结构，全部 `CREATE TABLE IF NOT EXISTS`，对旧库是空操作 |
| `V2__reference_data.sql` | 参考数据：10 个分类 + 欢迎公告（`INSERT IGNORE` / `WHERE NOT EXISTS`，幂等） |
| `V3__reservation_and_notification.sql` | 预约表 `reservation`（`uk_active` 唯一索引）+ 站内信表 `notification`（`uk_dedupe` 幂等）+ `book.held` 列 |

**改表结构的正确姿势**（不要再手工 ALTER，也不要回头改历史脚本）：

```bash
# 1. 新建迁移文件，版本号递增，描述用下划线分隔
backend/src/main/resources/db/migration/V4__add_user_avatar.sql
```

```sql
-- V4__add_user_avatar.sql
ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址';
```

```bash
# 2. 重启后端，日志会出现 Migrating schema ... to version "3 - add user avatar"
# 3. 提交时把这个文件一起提交，其他人拉代码重启即自动同步
```

几条硬规则：

- **历史脚本不可修改**，Flyway 会校验 checksum，改一个空格都会启动失败；
- 想改结构就**新增** `Vn`，出问题用后续版本修正，而不是删掉旧脚本；
- `baseline-on-migrate=true` + `baseline-version=0` 是给「早就用 `init.sql` 建过表」的库兜底：
  首次接管时打一条 BASELINE 记录，随后 V1/V2 因幂等而安全通过（日志里会提示表已存在，属预期）；
- 生产环境保留 `clean-disabled: true`，避免一条命令清库。

### 11. 结构化日志与 traceId

线上排障最痛的不是「报错了」，而是「不知道哪几条日志属于同一次请求」。
现在每个请求都有一个 32 位 traceId，从进入过滤器链开始，到鉴权、业务、异常堆栈，全部带同一个值。

**两种形态，一套配置**（`backend/src/main/resources/logback-spring.xml`）：

| profile | 输出 | 面向 |
|---|---|---|
| 默认（本地/测试） | 彩色文本，traceId 在方括号里 | 人眼 |
| `prod` | 单行 JSON（stdout + 滚动文件） | ELK / Loki / 云日志服务 |

```bash
# 本地：日志形如
2026-11-16 20:05:35.260 INFO  [e8b45e7dd48b4ff9b3eca1a2c47f4813] c.l.common.log.TraceIdFilter - GET /api/books/page -> 200 (151ms)

# 生产：SPRING_PROFILES_ACTIVE=prod，日志形如（已省略部分字段）
{"@timestamp":"2026-11-16T20:06:41.769+08:00","level":"INFO","message":"GET /api/books/page -> 200 (155ms)",
 "traceId":"1a5f02d103cf464ea5eea5f38f06c06a","userId":"1","username":"admin",
 "httpMethod":"GET","uri":"/api/books/page","httpStatus":"200","costMs":"155","clientIp":"127.0.0.1",
 "app":"library-backend","env":"prod"}
```

**实现要点**

- `TraceIdFilter` 用 `@Order(HIGHEST_PRECEDENCE)` 包在 Spring Security 链外层：
  未登录 401、token 过期这些「没进 Controller」的请求同样有 traceId；
- 上游（Nginx / 网关 / 前端）传 `X-Trace-Id` 时**优先沿用**，但必须通过白名单校验
  （8~64 位字母数字`-_`）——否则往头里塞换行就能伪造日志行（日志注入），也会破坏 JSON 结构；
- 响应头 `X-Trace-Id` 回传同一个值，500 的错误文案里也带「追踪号」，
  用户截图就等于给出了 `grep` 的检索键，不用再问「你几点点的」；
- `JwtAuthenticationFilter` / 登录成功后 `TraceContext.markUser` 补 `userId/username`，
  于是「谁」和「哪次请求」出现在同一行；
- 请求结束 `MDC.clear`（只清自己写的键）：Tomcat 线程池会复用线程，残留的 traceId 会污染下一个请求；
- 定时任务（`OverdueCheckTask`）没有 HTTP 入口，用 `TraceContext.beginNew()` 自己开一段，
  逾期扫描的整段日志同样能聚合；
- 访问日志按状态码分级：5xx → ERROR（可接告警）、4xx → WARN、其余 INFO；
  `/uploads/**` 与 `/actuator/**` 不打访问日志（一次列表页几十个 img、容器每 15s 探活，会刷屏），但仍带 traceId。

**日志落盘与容量**：`prod` 下同时写 stdout 和 `${LOG_DIR}/library-backend.log`，
按天 + 100MB 滚动、gzip 归档、保留 15 天、**总量封顶 2GB**（`totalSizeCap`）——
异常风暴时宁可丢旧日志，也不能把宿主机磁盘写满。容器里 `LOG_DIR=/app/logs` 并挂到命名卷 `backend-logs`。

**排障姿势**

```bash
# 用户报「保存图书失败，追踪号 3f4931dc...」→ 一条命令捞出整条链路
grep 3f4931dc6dd94d6ea8883466d04e0c7b logs/library-backend.log | jq -s '.[] | {t:@timestamp,l:level,m:message}'
```

**测试护栏**：`TraceIdFilterTest`（7 项）覆盖自动生成、上游透传、非法值拒绝、访问日志 MDC 字段、
500 文案含追踪号、请求后 MDC 清空、静默路径不刷日志。

---

## 十、下一步该加什么技术（按性价比排序）

> 判断标准：**解决当前真实痛点** > 技术流行度。以下每一项都注明「什么情况下才值得加」，避免过度设计。

### 已完成（本轮新增）

| 能力 | 实现 | 验证方式 |
|---|---|---|
| **统计可视化** | ECharts 6 按需引入（`utils/echarts.js`），统计页加借还趋势折线、分类占比环形图、借阅排行，后端配套 3 个统计接口 | 无头 Chrome 截图，控制台无报错；`borrow-trend?days=7` 返回补零后的完整 7 天 |
| **操作审计（AOP）** | `@Audit` 注解 + `OperationLogAspect`，记录操作人/模块/动作/URI/IP/入参/耗时/结果，密码字段打码；管理端「操作日志」页可按模块与结果筛选 | 实测成功与失败（`分类名称已存在`）两类记录均落库，读者访问返回 403 |
| **逾期自动处理** | `@Scheduled` 每日 01:00 刷新 `borrow_record.overdue` 并发布催还公告（同日幂等），另有管理员手动触发接口 | 手动触发两次：首次 `markedCount=1 / noticeCreated=true`，二次 `markedCount=0 / noticeCreated=false` |
| **CSV 导出** | `CsvUtil` 零依赖实现，UTF-8 BOM + 字段转义 | 导出 1376 字节文件，`xxd` 首三字节为 `efbbbf`，中文列名与书名正常 |
| **Excel 导出** | `ExcelUtil`（EasyExcel 3.3.4 无实体模型写法，锁版本因 3.4+ 要 Java 21），`/api/borrows/export?format=xlsx` 与 CSV 共用同一份取数 | `ExcelUtilTest` 4 项：POI 读回逐格断言（中文/数字类型/空数据/非法表名/1000 行流式）全过；前端导出按钮支持格式选择 |
| **Testcontainers 集成测试** | 真实 MySQL 容器 + Flyway 同源迁移，覆盖扣库存、缺货失败、并发不超借 | 本机无 Docker → `Skipped: 3`；CI 中真正执行 |
| **Flyway 数据库版本管理** | 表结构收敛到 `db/migration/V1、V2`，启动自动迁移并记录 `flyway_schema_history`；存量库靠 `baseline-on-migrate` + 幂等脚本平滑接管 | 存量库启动日志：`Creating Schema History table ... with baseline` → `Successfully applied 2 migrations`，53 本图书/15 条借阅数据无损 |
| **密码自助修改 + 登录防爆破** | `POST /api/auth/password` 校验原密码；登录按账号失败 5 次锁定 15 分钟、按 IP 每分钟限流 20 次（429） | 实测：第 1~4 次提示剩余次数，第 5 次返回 429；锁定期间正确密码同样被拒；改密后新密码可登录、旧密码失效 |
| **封面后台上传** | 本地磁盘存储 + `/uploads/**` 静态映射；扩展名白名单 + 2MB 上限 + 文件头魔数三重校验，UUID 命名避免路径穿越 | 正常 jpg 上传返回 URL 且匿名 GET 200；伪装成 .jpg 的文本、.md 文件均被拒 400；读者角色调用返回 403 |
| **图书批量导入（CSV）** | `CsvUtil.parse` 状态机解析（BOM/引号/内嵌换行/CRLF），逐行校验后批量入库；ISBN 已存在跳过、分类按名称自动创建、坏行逐条返回行号与原因 | 6 行测试数据：`total=6 success=2 skipped=2 failed=2`，失败原因分别为「书名为空」「定价不是有效数字」；空作者自动落「佚名」，新分类自动建并可立刻用于检索 |
| **结构化日志 + traceId** | `TraceContext`（MDC）+ `TraceIdFilter`（`HIGHEST_PRECEDENCE`）+ `logback-spring.xml` 双形态：dev 彩色文本、prod 单行 JSON；traceId 跨响应头与错误文案回传，定时任务同样开追踪 | 实测：401/400/200 三类请求各自打印带 traceId 的访问日志，上游传 `X-Trace-Id` 原样沿用、传非法值被替换；prod JSON 含 `traceId/userId/httpStatus/costMs` 等字段且无本机路径泄漏；日志能力测试 7 项通过 |
| **预约排队 + 到书通知** | V3 新增 `reservation` 表（`uk_active` 唯一索引实现「一人一书一条有效预约」）与 `book.held` 列，库存口径变成 `stock + held + borrowed = 总量`；归还时把回馆副本锁定给队首并发站内信，借书时 `lendHeld` 消耗锁定副本；`ReservationSweepTask` 每小时清扫过期预约并顺延 | 《艺术的故事》全链路：借完 → 预约排队第 1 → 还书后 `held=1` 且预约转「待取书」（保留 7 天）→ 读者借书后 `held=0 borrowed=2`、预约转「已借出」→ 归还后库存完全复原；重复预约被 `uk_active` 拦为 400 |
| **到期前站内提醒** | `DueReminderTask` 每日 09:00 扫 `due_date` 落在 3 天内的在借记录，逐条发站内信；窗口规则抽成 `DueSoonPolicy` 纯函数，`dedupe_key` 唯一索引保证同一条借阅同一应还日期只发一次；另给管理员 `POST /borrows/due-soon-remind` 手动补跑 | 默认 3 天窗口 `sent=0`（在借均在 30 天后到期）；放宽到 30 天 `sent=7`；同参数再跑 `sent=0` 证明幂等；`DueSoonPolicyTest` 7 项覆盖窗口两端边界与续借后键变化 |

### 第一优先级：下一步补什么（成本低、收益确定）

| 技术 | 解决什么问题 | 落地方式 |
|---|---|---|
| **滞纳金 / 逾期罚则** | 逾期只剩「禁止再借 + 公告点名」，没有成本约束，长期占用热门书的收益大于代价 | `borrow_record` 加 `fine_amount`，`OverdueCheckTask` 按「逾期天数 × 日费率 × 封顶」计算并写冻结标记；缴费记录单独一张表（这里只记账，不接支付） |
| **检索增强：分类树 + 相关推荐** | 53 本书靠关键词够用，馆藏上万后「同分类还有哪些」「这本书谁借过」才是真需求 | 分类加 `parent_id` 做两级树；详情页加「同分类推荐」与「借过这本书的读者还借了」，先用 SQL 排序，数据量上来再换 ES |

### 第二优先级：用户量或数据量上来后再加

| 技术 | 触发条件 | 说明 |
|---|---|---|
| **Redis** | 图书列表/分类树 QPS 变高，或要做登录态黑名单 | 缓存热点图书、缓存分类树；JWT 登出用 Redis 存黑名单（当前无状态 JWT 无法主动失效） |
| **Elasticsearch** | 图书过万，`LIKE '%关键字%'` 明显变慢 | 书名/作者/简介全文检索 + 拼音/同义词，MySQL 只存主数据 |
| **RabbitMQ / Kafka** | 借阅成功后要发短信/站内信/积分，主链路不该被拖慢 | 借书成功后发事件，异步消费；当前直接在事务里做会拉长事务时间 |
| **MinIO / OSS** | 多实例部署时本地封面目录无法共享，或要上传读者头像/附件 | 封面已抽象成 `FileStorageService`，换成对象存储只需新增一个实现类；届时可改为预签名 URL 前端直传 |
| **Prometheus + Grafana** | 需要看 QPS、P95 延迟、慢 SQL 趋势 | actuator 已暴露 `/actuator/prometheus`（需加 `micrometer-registry-prometheus` 依赖），配 Grafana 面板 + 告警 |

### 第三优先级：真要做成生产系统才需要

| 技术 | 说明 |
|---|---|
| **Spring Cloud Gateway / Nginx 网关层** | 统一鉴权、限流、灰度；当前单体应用直连即可 |
| **Sentinel / Resilience4j** | 限流熔断；登录已有本地内存版限流，多实例部署时再换成 Redis 计数以共享状态 |
| **Kubernetes + Helm** | 多实例、滚动发布、自动扩缩容；单机 `docker compose` 足够时不要上 |
| **SkyWalking / OpenTelemetry** | 全链路追踪，微服务化之后才真正有价值 |

### 明确不建议现在做的

- **微服务拆分**：当前 6 张表、40 个类，拆开只会增加分布式事务和运维成本；
- **CQRS / 事件溯源**：图书管理的读写模型差异不大，收益接近于零；
- **引入 GraphQL**：接口数量有限，RESTful + Swagger 已经够用，GraphQL 会让缓存和权限更复杂。

### 一句话建议

本轮补的是**业务闭环**：预约排队把「借完了怎么办」接上，站内信把「到馆了谁知道」「快到期了谁提醒」接上，
库存口径也从 `stock + borrowed` 扩成 `stock + held + borrowed`，三者任何时刻都该等于馆藏总量——
这是整个系统里最容易算错、也最难在页面上发现的地方，所以每一处扣减都写成带条件的 UPDATE 并用单测守住。
下一步最值得做的是**滞纳金**：现在逾期只损失「不能再借」的资格，一本热门书压在手里一个月几乎没有成本。
Redis、ES、消息队列等，等真有并发或数据量压力再说。
