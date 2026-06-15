# ==========================================
# 健康管理服务端 Docker 镜像
# 多阶段构建，优化镜像大小
# ==========================================

# 阶段 1: 构建阶段
FROM node:20-alpine AS builder

# 安装构建依赖
RUN apk add --no-cache python3 make g++

# 设置工作目录
WORKDIR /app

# 复制依赖配置文件
COPY package.json package-lock.json ./

# 安装所有依赖（包括开发依赖，构建需要）
RUN npm ci

# 复制 Prisma schema
COPY prisma ./prisma/

# 生成 Prisma Client
RUN npx prisma generate

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# ==========================================
# 阶段 2: 生产阶段
FROM node:20-alpine AS production

# 安装运行时依赖
RUN apk add --no-cache \
    curl \
    tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

# 创建非 root 用户
RUN addgroup -g 1001 -S nodejs \
    && adduser -S nestjs -u 1001 -G nodejs

# 设置工作目录
WORKDIR /app

# 复制依赖配置文件
COPY package.json package-lock.json ./

# 仅安装生产依赖
RUN npm ci --only=production && npm cache clean --force

# 从构建阶段复制 Prisma Client
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /app/node_modules/@prisma ./node_modules/@prisma

# 从构建阶段复制构建产物
COPY --from=builder /app/dist ./dist

# 从构建阶段复制 Prisma schema（用于 prisma migrate）
COPY --from=builder /app/prisma ./prisma

# 创建日志目录
RUN mkdir -p /app/logs && chown -R nestjs:nodejs /app/logs

# 设置构建参数
ARG NODE_ENV=production
ARG BUILD_VERSION=unknown

# 设置环境变量
ENV NODE_ENV=${NODE_ENV} \
    BUILD_VERSION=${BUILD_VERSION} \
    PORT=3000

# 切换到非 root 用户
USER nestjs

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:3000/api/health || exit 1

# 启动应用
CMD ["node", "dist/main"]
