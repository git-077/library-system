# 部署手册

> 目标：把本项目放到一台 Linux 服务器上，别人打开网址就能用。
> 本机开发怎么跑，见 `README.md` 第一节；本文只讲「上线」。
> 举例环境：Ubuntu 22.04 / 2 核 2G / 有公网 IP。命令可直接复制，需替换的地方用 `<>` 标出。

## 0. 先看懂：部署在拼什么

```
浏览器
  │  http://<域名或公网IP>/            静态文件
  ├───────────────► Nginx :80 / :443
  │                    ├── /            → dist/index.html
  │                    ├── /assets/     → 带 hash 的 js/css，长缓存
  │                    ├── /covers/     → 53 张预置封面（构建时从 frontend/public 复制进 dist）
  │                    ├── /api/        → 反代 http://127.0.0.1:8080
  │                    └── /uploads/    → 反代 http://127.0.0.1:8080（后台后台上传的封面，落磁盘）
  │
  └── 所有动态请求 → Spring Boot :8080 ──► MySQL :3306
```

四句话记住：

1. 前端**必须 build 成静态文件**交给 Nginx，不能用 `npm run dev` 上线。
2. 后端就是一个 jar，跑在 8080；Nginx 挡在前面反代 `/api` 和 `/uploads`，前后端同源，不涉及跨域。
3. **表结构不用手工建**：后端启动时 Flyway 自动执行 `backend/src/main/resources/db/migration/V1~V3`。
4. **数据库和上传目录必须持久化**，否则容器/机器重建就丢。

## 1. 选哪条路

| 你的情况 | 选 | 理由 |
|---|---|---|
| 有 Linux 云服务器 | **路线 A：Docker Compose**（推荐） | 一条命令起三个服务，不用在服务器装 JDK/Maven/Node/MySQL，版本不会错 |
| 服务器装不了 Docker，或想看清每个部件 | 路线 B：Nginx + systemd + 本机 MySQL | 每个部件单独看日志，排错直观，也是理解 A 的最好方式 |
| 只想在自己电脑演示 | README 第一节 dev 模式 | 最省事，但不是部署 |

最低配置：**2 核 2G / 20G 磁盘**。1 核 1G 会在 Maven 构建或 JVM 启动时 OOM，至少先加 2G swap。

云服务器上还要做两件事（初学者最常卡在这里）：

- 控制台**安全组**放行 `22`、`80`、`443`；**不要**放行 `3306` 和 `8080`。
- 有域名的话，加一条 A 记录指向公网 IP；没有域名就用 `http://<公网IP>` 访问。

---

## 2. 路线 A：Docker Compose

### 2.1 装 Docker（一次性）

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# 退出 SSH 重新登录（或执行 newgrp docker），让用户组生效
docker version && docker compose version
```

国内网络访问 `get.docker.com` 慢的话，改用阿里云镜像源安装，或给 Docker 配镜像加速器
（`/etc/docker/daemon.json` 里加 `registry-mirrors`），否则拉 `mysql:8.0` 会很慢。

### 2.2 把代码放到服务器

```bash
# 方式一：Git（推荐，以后升级就是 git pull）
git clone <你的仓库地址> library-system
cd library-system

# 方式二：本地打包上传（没有仓库时）
# 本地：bash scripts/make-release.sh
#       scp ../library-system-src.tgz root@<服务器IP>:/opt/
# 服务器：mkdir -p /opt/library-system && tar -xzf /opt/library-system-src.tgz -C /opt/library-system
```

> `make-release.sh` 用 `git archive` 打包，**只包含已提交的文件**。改了没 commit 的内容不会进包，先提交再打包。

### 2.3 配置密钥（必做，不要跳过）

```bash
cp .env.example .env
nano .env
```

改这三行，其余保持默认：

```ini
MYSQL_ROOT_PASSWORD=<用 openssl rand -base64 18 生成的串>
JWT_SECRET=<用 openssl rand -base64 48 生成的串>          # 长度必须 >= 32 字符
CORS_ORIGINS=http://<你的域名或公网IP>
```

生成密钥：

```bash
openssl rand -base64 18    # 数据库密码
openssl rand -base64 48    # JWT 密钥
```

`JWT_SECRET` 用默认值等于把签发权交给所有人——任何人都能自己造一个管理员 token。

2 核 2G 的机器再加一行，避免 JVM 默认按 75% 吃满内存、和 MySQL 抢：

```ini
JAVA_OPTS=-Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai
```

### 2.4 起服务

```bash
docker compose up -d --build
docker compose ps
```

首次构建要下 Maven 依赖和 npm 包，约 3~10 分钟。`ps` 里三个服务的 STATUS 都应是 `Up (healthy)`。

看后端有没有真的连上库：

```bash
docker compose logs backend | grep -E "Flyway|Migrating|Started LibraryApplication"
```

应能看到 `Successfully applied 3 migrations` 和 `Started LibraryApplication`。

> 80 端口被占用（比如服务器上已有别的站点）时，把 `docker-compose.yml` 里前端的
> `"80:80"` 改成 `"8080:80"`，之后用 `http://IP:8080` 访问。

### 2.5 验收（五条，全过才算部署成功）

```bash
curl -s -o /dev/null -w "首页   %{http_code}\n" http://127.0.0.1/
curl -s -o /dev/null -w "健康   %{http_code}\n" http://127.0.0.1/healthz
curl -s -o /dev/null -w "API    %{http_code}\n" http://127.0.0.1/api/books/page    # 期望 401
curl -s -o /dev/null -w "封面   %{http_code}\n" http://127.0.0.1/covers/9787111213828.jpg
curl -s -X POST http://127.0.0.1/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | head -c 160
```

预期：`200 / 200 / 401 / 200`，最后一条返回带 `"token"` 的 JSON。
`/api/books/page` 返回 **401 才是对的**——说明请求已经穿过 Nginx 到达后端，只是没带 token。

浏览器打开 `http://<域名或IP>`，用 `admin / admin123` 登录，能看到图书检索页和封面图，即完成。

### 2.6 灌演示数据（可选）

空库首次启动时，后端会自动创建 `admin/admin123`、`reader/123456`、`reader2/123456`。
想用 45 本书 + 11 个用户 + 15 条借阅的完整演示数据：

```bash
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" library_system_db' < sql/seed-demo-data.sql
```

脚本按 ISBN 先删后建，可重复执行。

### 2.7 日常运维

```bash
docker compose ps                      # 状态
docker compose logs -f backend         # 跟日志（prod 是单行 JSON）
docker compose restart backend         # 重启后端
docker compose down                    # 停止（数据卷保留）
docker compose down -v                 # 停止并删除数据（不可恢复，慎用）
```

**升级发布**：

```bash
git pull
docker compose up -d --build           # 只重建有变化的镜像
```

新版本里的 Flyway 脚本会在启动时自动执行，`mysql-data` 卷不动，数据不丢。

**看某一次请求的完整链路**（日志带 traceId，用户报错时可让他提供页面上的追踪号）：

```bash
docker compose logs backend | grep <追踪号>
```

**备份与恢复**（建议加进 crontab，每天一次）：

```bash
# 数据库
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction library_system_db' \
  | gzip > backup-db-$(date +%F).sql.gz

# 运行期上传的封面（不在镜像里，丢了就是永久 404）
docker run --rm -v library-system_backend-uploads:/data -v "$PWD":/backup alpine \
  tar czf /backup/backup-uploads-$(date +%F).tar.gz -C /data .

# 恢复
gunzip -c backup-db-2026-11-16.sql.gz \
  | docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" library_system_db'
```

---

## 3. 路线 B：不用 Docker（Nginx + systemd + MySQL）

适合想在服务器上装最小依赖、或者需要逐个部件排查的场景。

### 3.1 装依赖

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless nginx mysql-server
java -version && nginx -v && mysql --version
```

### 3.2 建库和专用账号

不要用 root 跑应用：

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS library_system_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'library'@'127.0.0.1' IDENTIFIED BY '<强密码>';"
sudo mysql -e "GRANT ALL PRIVILEGES ON library_system_db.* TO 'library'@'127.0.0.1'; FLUSH PRIVILEGES;"
```

MySQL 8 的 root 默认走 `auth_socket`，所以 `sudo mysql` 免密；应用用新建的 `library` 账号连。

### 3.3 在本地构建产物（别在 2G 服务器上跑 Maven + npm）

```bash
# 项目根目录
mvn -f backend/pom.xml clean package -DskipTests
npm --prefix frontend ci
npm --prefix frontend run build

# 确认产物真的完整：
ls -lh backend/target/library-backend-1.0.0.jar
ls frontend/dist/assets | wc -l      # 应为 30 多，绝对不能是 0
```

### 3.4 上传

```bash
ssh root@<服务器IP> "mkdir -p /opt/library /var/www/library"
scp backend/target/library-backend-1.0.0.jar root@<服务器IP>:/opt/library/app.jar
scp -r frontend/dist/. root@<服务器IP>:/var/www/library/
```

### 3.5 建运行用户与目录

```bash
sudo useradd -r -s /usr/sbin/nologin -d /opt/library library
sudo mkdir -p /opt/library/logs /opt/library/uploads
sudo chown -R library:library /opt/library
sudo chown -R www-data:www-data /var/www/library
```

`uploads` 必须是可写的，否则后台上传封面会 500。

### 3.6 环境变量文件

```bash
sudo tee /opt/library/backend.env >/dev/null <<'EOF'
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USERNAME=library
MYSQL_PASSWORD=<强密码>
JWT_SECRET=<至少 32 位随机串>
CORS_ORIGINS=http://<域名或公网IP>
SPRING_PROFILES_ACTIVE=prod
LOG_DIR=/opt/library/logs
UPLOAD_DIR=/opt/library/uploads
EOF
sudo chmod 600 /opt/library/backend.env
sudo chown library:library /opt/library/backend.env
```

### 3.7 systemd 托管

`/etc/systemd/system/library-backend.service`：

```ini
[Unit]
Description=Library System Backend
After=network-online.target mysql.service
Wants=network-online.target

[Service]
Type=simple
User=library
WorkingDirectory=/opt/library
EnvironmentFile=/opt/library/backend.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -Duser.timezone=Asia/Shanghai -jar /opt/library/app.jar
Restart=always
RestartSec=5
SuccessExitStatus=143
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now library-backend
sudo systemctl status library-backend --no-pager
sudo journalctl -u library-backend -f      # 看启动日志
```

`-Xmx512m` 是给 2G 机器留余量；机器只有 1G 就降到 `-Xmx384m` 并加 swap。

### 3.8 Nginx 站点

`/etc/nginx/sites-available/library`：

```nginx
server {
    listen 80;
    server_name <域名或公网IP>;

    root /var/www/library;
    index index.html;

    client_max_body_size 5m;      # 封面限制 2MB，这里留余量；不设上传会报 413

    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain text/css application/javascript application/json image/svg+xml;

    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
        try_files $uri =404;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        expires 1d;
    }

    # 少了这行，在 /books 直接刷新会 404（前端是 history 路由）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 探活端点：负载均衡 / uptime 监控 / 第 2.5 节的验收命令都打这里
    location = /healthz {
        access_log off;
        add_header Content-Type text/plain;
        return 200 "ok\n";
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/library /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

### 3.9 防火墙

```bash
sudo ufw allow OpenSSH
sudo ufw allow 'Nginx Full'
sudo ufw enable
```

云厂商安全组同样只放行 22/80/443。

### 3.10 HTTPS（有域名就一定要做）

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d lib.example.com
```

certbot 会自动改 Nginx 配置并续期。之后把 `/opt/library/backend.env` 里的
`CORS_ORIGINS` 改成 `https://lib.example.com`，再 `sudo systemctl restart library-backend`。

---

## 4. 上线前必改清单

| 项 | 不改的后果 | 怎么改 |
|---|---|---|
| `JWT_SECRET` | 默认值是公开的，任何人都能伪造管理员 token | 换成 `openssl rand -base64 48` |
| 数据库密码 | 弱口令 + 端口若暴露 = 整库被拖走 | 换强密码，且只监听 127.0.0.1 |
| `admin` 密码 | 还是 `admin123`，等于没有后台 | 登录后在「个人中心 - 账户安全」改掉 |
| `CORS_ORIGINS` | 写 `*` 时任意站点都能带 cookie 调接口 | 写真实域名；同源部署下其实用不到跨域 |
| 3306 / 8080 对公网 | 数据库和 API 直接暴露，被扫到就是攻击面 | 安全组不放行；compose 里已绑 `127.0.0.1` |
| Swagger 公网可访问 | `/swagger-ui.html` 把全部接口和参数结构公开 | 不需要就给 Nginx 加 IP 白名单（见下） |
| 备份 | 磁盘坏了/误删库，数据全没 | 按 2.7 的备份命令加进 crontab |

限制 Swagger（可选，接在 Nginx `server` 块里）：

```nginx
location ~ ^/(swagger-ui|v3/api-docs) {
    allow <你的办公 IP>;
    deny all;
    proxy_pass http://127.0.0.1:8080;
}
```

---

## 5. 排错对照表

| 现象 | 最可能的原因 | 怎么确认 / 怎么修 |
|---|---|---|
| 打开是 Nginx 默认欢迎页 | 站点没启用，或 `default` 没删 | `ls /etc/nginx/sites-enabled/`；删 default 后 `nginx -t && systemctl reload nginx` |
| 页面白屏，控制台 `/assets/xxx.js` 404 | `dist` 没传全，或传到了错误目录 | 服务器上 `ls /var/www/library/assets`；重新 `scp -r frontend/dist/. ...` |
| 首页能开，刷新 `/books` 变 404 | 缺 `try_files ... /index.html` | 对照 3.8 的 `location /` |
| 接口 502 Bad Gateway | 后端没起 / 端口不对 / 崩了 | `systemctl status library-backend`、`journalctl -u library-backend -n 50` |
| 接口 404 但后端是活的 | 反代路径写成 `/api`（少了尾斜杠）或 `proxy_pass` 带了 URI | 用 `location /api/ { proxy_pass http://127.0.0.1:8080; }`（末尾不带 `/`） |
| 登录一直「用户名或密码错误」 | 连到了另一个库（比如本机旧库），账号不存在 | 看后端启动日志的 jdbc url；确认 `MYSQL_*` 指向服务器库 |
| 封面全是灰底占位图 | `dist/covers` 没上传，或库里 `cover_url` 指向 `/uploads/...` 但文件没了 | `curl -I http://IP/covers/9787111213828.jpg` |
| 后台上传封面 413 | Nginx 默认只允许 1MB | `client_max_body_size 5m;` 后 reload |
| 上传成功但刷新后图 404 | `uploads` 目录不可写，或容器没挂卷 | 路线 A 检查 `backend-uploads` 卷；路线 B 检查目录属主 |
| 接口报 CORS 错误 | 前端没走同源 `/api`（比如直接写了 `http://IP:8080`） | 前端请求统一用相对路径，交给 Nginx |
| 时间差 8 小时 | 容器/数据库时区不是东八区 | compose 已设 `TZ=Asia/Shanghai`；宿主机 `timedatectl set-timezone Asia/Shanghai` |
| 报 500 且提示「追踪号 xxxx」 | 后端内部异常 | `docker compose logs backend \| grep xxxx` 或 `grep xxxx /opt/library/logs/library-backend.log` |
| 构建时 Killed / OOM | 2G 机器上 Maven 构建吃内存 | 本地构建后只传 jar；或加 swap |

---

## 6. 本项目特有的三个坑

1. **`dist` 里必须有 `assets/` 目录**。构建被中断时会留下「只有 covers + index.html」的半成品（本项目实际踩过），
   上传后就是白屏。判断标准：`ls frontend/dist/assets | wc -l` 必须是 30 多，而不是 0 或报错。
2. **`/covers/` 和 `/uploads/` 是两套东西**。前者是 53 张预置封面，在 `frontend/public/covers`，
   构建时复制进 `dist`，属于「代码」；后者是管理员后台上传的，属于「运行数据」，必须持久化 + 单独备份。
   只备份数据库不备份 `uploads`，恢复后这些书就是坏图。
3. **没有 `application-prod.yml`**。`SPRING_PROFILES_ACTIVE=prod` 只切换日志格式（dev 彩色文本 / prod 单行 JSON），
   所有生产配置都靠环境变量（`MYSQL_*`、`JWT_SECRET`、`CORS_ORIGINS`、`LOG_DIR`、`UPLOAD_DIR`）。
   所以「配了 prod 但密码还是默认值」是真实存在的风险，第 4 节的清单要逐条过。
