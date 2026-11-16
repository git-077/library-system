# 图书管理系统（Spring Boot 3 + Vue 3 前后端分离）

> 技术栈：Spring Boot 3.2.5 / MyBatis-Plus 3.5.5 / MySQL 8.0 / Spring Security + JWT / Spring AOP（审计日志）
> / Vue 3 + Vite 5 + Element Plus + Pinia / ECharts 6（统计图表）
>
> **运行状态：本机已实测跑通**（Java 17.0.8、Maven 3.6.1、Node 22、MySQL 8.0.33）。
> 后端接口自检 13 项 + 写接口回归 17 项全部通过；测试 14 项（11 个单测通过，3 个 Testcontainers 集成用例本机无 Docker 自动跳过）；
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

**① 初始化数据库**（只需执行一次，库名 `library_system_db`，不会影响你已有的其他库）

```bash
cd library-system
mysql -uroot -p123456 < sql/init.sql
# 可选：灌入演示数据（10 个分类 / 45 本演示图书 / 11 个用户 / 15 条借阅 / 11 条收藏）
# 脚本按 ISBN 先删后建，可重复执行，不会产生重复数据
mysql -uroot -p123456 < sql/seed-demo-data.sql
```

> 如果你的 root 密码不是 `123456`：命令换成 `mysql -uroot -p < sql/init.sql` 后输入你的密码，
> 并同步修改 `backend/src/main/resources/application.yml` 里的 `password` 默认值（或启动时设置环境变量 `MYSQL_PASSWORD`）。

**② 启动后端**（新开一个终端）

```bash
cd library-system/backend
mvn spring-boot:run
```

看到 `Started LibraryApplication` 即成功，服务在 http://localhost:8080 。
首次启动会自动创建默认账号。

**③ 启动前端**（再开一个终端）

```bash
cd library-system/frontend
npm install        # 首次需要，约 1~5 分钟（视网络）
npm run dev
```

浏览器打开 **http://localhost:5173** 即可。

> Windows 用户也可以直接双击项目根目录的 `start-local.bat`，会自动开两个窗口分别拉起前后端；
> Git Bash / macOS / Linux 用 `bash start-local.sh`。

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

---

## 三、目录结构

```
library-system/
├── docker-compose.yml            # MySQL + 后端 + 前端编排
├── start-local.bat / .sh         # 本地一键启动脚本（含端口预检、复用已运行服务）
├── stop-local.sh                 # 停止 start-local.sh 拉起的进程
├── .gitignore                    # 排除 target/node_modules/.idea/日志等
├── package.json / commitlint.config.mjs  # 根目录仅放 Git 工具链（提交信息规范校验）
├── scripts/hooks/commit-msg      # 依赖零安装的 commit-msg 钩子（核心逻辑，可版本化）
├── sql/init.sql                  # 建库建表 + 初始图书/分类/公告数据
├── sql/seed-demo-data.sql        # 演示数据（10 分类 / 45 图书 / 11 用户 / 15 借阅），可重复执行
├── scripts/verify-api.mjs        # 接口自检脚本（node scripts/verify-api.mjs）
├── scripts/verify-crud.mjs       # 写接口回归脚本（图书/分类/公告的增删改与校验，含数据清理）
├── backend/
│   ├── pom.xml / Dockerfile / .dockerignore
│   └── src/
│       ├── main/java/com/library/
│       │   ├── LibraryApplication.java
│       │   ├── common/                       # 通用能力，不含任何业务语义
│       │   │   ├── config/                   # MybatisPlus / Jackson / OpenApi / SecurityConfig / DataInitializer
│       │   │   ├── constant/RoleConstants    # 角色与默认密码常量，禁止在业务代码里写字面量
│       │   │   ├── log/Audit.java            # @Audit 审计注解（切面实现放在 module.log）
│       │   │   ├── exception/                # BizException + GlobalExceptionHandler
│       │   │   ├── result/Result.java        # 统一响应 {code,message,data,timestamp}
│       │   │   ├── security/                 # JwtUtil / JwtAuthenticationFilter / LoginUser / SecurityUtil
│       │   │   ├── util/CsvUtil.java         # CSV 导出（UTF-8 BOM + 字段转义）
│       │   │   └── web/RootController.java   # 根路径探针，避免把 8080 当页面地址访问
│       │   └── module/                       # 业务模块：统一 controller / dto / entity / mapper / service(+impl)
│       │       ├── auth/       # 登录 / 注册 / 当前用户（AuthService）
│       │       ├── user/       # 用户档案 CRUD（UserService）
│       │       ├── category/   # 分类
│       │       ├── book/       # 图书（BookMapper 内含条件扣减库存 SQL）
│       │       ├── borrow/     # 借阅事务（借书 / 还书 / 续借）+ task/ 逾期扫描定时任务
│       │       ├── notice/     # 公告
│       │       ├── favorite/   # 收藏
│       │       ├── portal/     # 读者门户聚合（PortalService + PortalOverviewVO）
│       │       ├── stat/       # 后台统计（概览 + 趋势 + 分类分布 + 借阅排行）
│       │       └── log/        # 操作日志（Audit 切面 + 查询接口）
│       ├── main/resources/application.yml
│       └── test/java/com/library/            # 单元测试（JWT、接口契约）
└── frontend/
    ├── package.json / vite.config.js / index.html / Dockerfile / nginx.conf
    ├── eslint.config.mjs / .prettierrc.json           # 静态检查与格式化配置
    └── src/
        ├── main.js / App.vue
        ├── styles/theme.css              # 设计令牌：暖米色 / 深绿 / 金色，全局样式
        ├── router/index.js               # 路由 + 守卫（登录态、角色）
        ├── stores/user.js                # Pinia：token / 用户信息
        ├── utils/request.js              # Axios 拦截器（携带 JWT、统一响应、401 跳转）
        ├── utils/index.js                # 借阅状态与逾期判断
        ├── utils/echarts.js              # ECharts 按需注册 + 统一配色
        ├── components/EChart.vue         # 图表封装（挂载 / 更新 / 尺寸自适应 / 销毁）
        ├── api/                          # auth/book/category/user/borrow/notice/favorite/stat/portal
        └── views/                        # Login / Layout / Dashboard / BookList / BookManage /
                                          # CategoryManage / UserManage / BorrowManage / NoticeList /
                                          # OperationLog / Profile
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
- **CSV 导出**：`CsvUtil` 零依赖实现，写 UTF-8 BOM 保证 Excel 打开不乱码，含逗号/引号的书名正确转义。

---

## 六、API 一览

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 匿名 | 登录，返回 JWT |
| POST | /api/auth/register | 匿名 | 注册（读者角色） |
| GET | /api/auth/me | 登录 | 当前用户信息 |
| GET | /api/users/page | ADMIN | 用户分页（用户名模糊） |
| POST / PUT / DELETE | /api/users/** | ADMIN | 新增 / 编辑 / 启禁用 / 重置密码 / 软删除 |
| GET | /api/books/page | 登录 | 图书分页检索（keyword / categoryId / status） |
| GET | /api/books/{id} | 登录 | 图书详情 |
| POST / PUT / DELETE | /api/books/** | ADMIN | 图书新增 / 编辑 / 上下架 / 软删除 |
| GET | /api/categories | 登录 | 分类列表 |
| POST / PUT / DELETE | /api/categories/** | ADMIN | 分类管理 |
| POST | /api/borrows?bookId= | 登录 | 借书 |
| PUT | /api/borrows/{id}/return | 登录 | 还书（本人或管理员） |
| PUT | /api/borrows/{id}/renew | 登录 | 续借（本人或管理员） |
| GET | /api/borrows/my | 登录 | 我的借阅历史（分页） |
| GET | /api/borrows/page | ADMIN | 全部借阅记录（分页） |
| POST | /api/borrows/overdue-check | ADMIN | 手动触发逾期扫描（刷新标记 + 发布催还公告，幂等） |
| GET | /api/borrows/export | ADMIN | 导出借阅记录 CSV（带 BOM，Excel 可直接打开） |
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
3. **重建数据**：`mysql -uroot -p你的密码 -D library_system_db -e "DROP DATABASE library_system_db;"` 后重新执行 `sql/init.sql`。
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
- **图书详情弹窗**：左侧书封、右侧书名/作者/两列元信息（ISBN、分类、出版社、库存、可借）、简介，底部「借阅本书」按钮（已借完时禁用）。
- **我的借阅 / 系统公告 / 个人中心**：同色系卡片式排版，借阅记录带状态标签与归还、续借操作。

**管理端（后台形态）**——管理员登录后进入 `/dashboard`，左侧深绿侧边栏（9 个菜单，按角色过滤）+ 浅色内容区：

- **数据统计**：六个指标卡（馆藏种类、藏书总量、注册读者、在借册数、逾期未还、累计借阅），数字用无衬线粗体，单位用小号上标。
- **图书 / 分类 / 用户 / 借阅 / 公告管理**：统一「查询区卡片 + 表格卡片」结构；操作列按钮并排不换行，分类页用文字按钮减重。

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
mvn test                            # 14 个测试：11 个单测通过 + 3 个集成用例（无 Docker 时自动跳过）
mvn test -Dtest=JwtUtilTest         # 只跑某个类
```

| 测试类 | 覆盖内容 |
|---|---|
| `JwtUtilTest` | token 生成/解析、载荷不丢失、篡改/过期/换密钥/格式非法均返回 null |
| `AuthControllerTest` | 登录成功返回统一结构、空用户名被校验拦截、密码错误转换业务异常、注册用户名过短被拦截、未登录访问 `/api/auth/me` 返回 401 |
| `BorrowTransactionIntegrationTest` | **Testcontainers + 真实 MySQL**：借书扣库存并生成记录、库存为 0 时失败且不留脏数据、库存 1 时两人并发只能成功 1 人（不超借） |

前两个类**不依赖 Spring 容器和数据库**（standalone MockMvc + ReflectionTestUtils），秒级跑完，作为「接口格式被改坏」的护栏。
集成测试用 `@Testcontainers(disabledWithoutDocker = true)`：本机没装 Docker 时会整体跳过（显示 `Skipped: 3`），
CI（ubuntu runner 自带 Docker）里会真正拉起 `mysql:8.0.33` 并用**仓库里的 `sql/init.sql`** 建表，保证测试与生产同源。
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
| **Testcontainers 集成测试** | 真实 MySQL 容器 + 仓库同源 `init.sql`，覆盖扣库存、缺货失败、并发不超借 | 本机无 Docker → `Skipped: 3`；CI 中真正执行 |

### 第一优先级：现在就该补（成本低、收益确定）

| 技术 | 解决什么问题 | 落地方式 |
|---|---|---|
| **Logback 结构化日志 + traceId** | 线上出问题只有一行「系统异常」，定位不到是哪个用户哪次请求（审计日志只覆盖写操作） | `MDC.put("traceId", UUID)` 放在 `JwtAuthenticationFilter` 里，`logback-spring.xml` 输出 JSON 格式，接入 ELK / Loki |
| **Flyway 或 Liquibase** | 现在建表靠手工执行 `init.sql`，多人环境版本不一致、改字段靠记忆（本轮加 `operation_log` 表、`borrow_record.overdue` 列就是手工 ALTER 的） | 加 `flyway-core`，把 `sql/init.sql` 拆成 `V1__init.sql`、`V2__operation_log.sql`，启动自动迁移并记录 `flyway_schema_history` |
| **登录防暴力破解** | 登录接口目前不限速，可被撞库 | 最省事的做法：Redis 记 `login:fail:{username}` 计数，5 次锁定 15 分钟；或不引 Redis，用 Caffeine 本地缓存 + 固定窗口 |

### 第二优先级：用户量或数据量上来后再加

| 技术 | 触发条件 | 说明 |
|---|---|---|
| **Redis** | 图书列表/分类树 QPS 变高，或要做登录态黑名单 | 缓存热点图书、缓存分类树；JWT 登出用 Redis 存黑名单（当前无状态 JWT 无法主动失效） |
| **Elasticsearch** | 图书过万，`LIKE '%关键字%'` 明显变慢 | 书名/作者/简介全文检索 + 拼音/同义词，MySQL 只存主数据 |
| **RabbitMQ / Kafka** | 借阅成功后要发短信/站内信/积分，主链路不该被拖慢 | 借书成功后发事件，异步消费；当前直接在事务里做会拉长事务时间 |
| **MinIO / OSS** | 要上传图书封面、读者头像 | 对象存储 + 前端直传（预签名 URL），别把图片存进 MySQL 或容器本地磁盘 |
| **Prometheus + Grafana** | 需要看 QPS、P95 延迟、慢 SQL 趋势 | actuator 已暴露 `/actuator/prometheus`（需加 `micrometer-registry-prometheus` 依赖），配 Grafana 面板 + 告警 |

### 第三优先级：真要做成生产系统才需要

| 技术 | 说明 |
|---|---|
| **Spring Cloud Gateway / Nginx 网关层** | 统一鉴权、限流、灰度；当前单体应用直连即可 |
| **Sentinel / Resilience4j** | 限流熔断；登录接口现在没有防暴力破解，最简单的做法是先加 Redis 计数 + 锁定 15 分钟 |
| **Kubernetes + Helm** | 多实例、滚动发布、自动扩缩容；单机 `docker compose` 足够时不要上 |
| **SkyWalking / OpenTelemetry** | 全链路追踪，微服务化之后才真正有价值 |

### 明确不建议现在做的

- **微服务拆分**：当前 6 张表、40 个类，拆开只会增加分布式事务和运维成本；
- **CQRS / 事件溯源**：图书管理的读写模型差异不大，收益接近于零；
- **引入 GraphQL**：接口数量有限，RESTful + Swagger 已经够用，GraphQL 会让缓存和权限更复杂。

### 一句话建议

本轮已经把「统计可视化 + 操作审计 + 逾期定时任务 + CSV 导出 + Testcontainers 集成测试」补齐，项目在**功能完整度**和**工程规范**上都不缺东西了。
剩下最值得做的是 **Flyway（建表不再靠手工 ALTER）+ 结构化日志（traceId 串起一次请求）**，
这两项直接对应「改表结构靠记忆」「线上排障只能靠猜」的现实痛点。Redis、ES、消息队列等，等真有并发或数据量压力再说。
