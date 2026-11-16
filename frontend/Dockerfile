# 前端多阶段构建：Node 编译 Vue -> Nginx 托管静态资源
FROM node:20-alpine AS build
WORKDIR /app

# 先复制依赖清单，利用 Docker 层缓存（源码变动不必重装依赖）
COPY package*.json ./
# 使用国内镜像加速；如需官方源可删除下一行
RUN npm config set registry https://registry.npmmirror.com \
    && npm install --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# 覆盖默认站点配置：静态托管 + /api 反代 backend:8080
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
