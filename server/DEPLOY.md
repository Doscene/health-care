# 服务端部署指南

## 文件说明

| 文件 | 说明 |
|------|------|
| `Jenkinsfile` | Jenkins 流水线配置文件 |
| `Dockerfile` | Docker 镜像构建文件 |
| `deploy.sh` | 服务器端部署脚本 |

## 前置条件

### 1. Jenkins 配置

#### 安装必要插件
- Docker Pipeline
- SSH Agent
- NodeJS Plugin

#### 配置 NodeJS 工具
1. 进入 Jenkins -> 系统管理 -> 全局工具配置
2. 添加 NodeJS 安装，名称设为 `NodeJS-20`
3. 选择 NodeJS 20.x 版本

#### 配置凭据
1. **SSH 凭据** (ID: `ssh-credentials`)
   - 类型: SSH Username with private key
   - 用于连接部署服务器

2. **Docker 仓库凭据** (ID: `docker-credentials`)
   - 类型: Username with password
   - 用于推送镜像到私有仓库（可选）

### 2. 部署服务器配置

#### 安装 Docker
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 启动 Docker
sudo systemctl enable docker
sudo systemctl start docker
```

#### 创建部署目录
```bash
sudo mkdir -p /opt/healthcare/{server,logs,backup}
sudo chown -R $USER:$USER /opt/healthcare
```

#### 配置环境变量
创建 `/opt/healthcare/.env` 文件：
```env
# 数据库配置
DATABASE_URL="mysql://user:password@localhost:3306/healthcare"

# JWT 配置
JWT_SECRET="your-production-jwt-secret-here"

# Redis 配置
REDIS_URL="redis://localhost:6379"

# MinIO 配置
MINIO_ENDPOINT="localhost"
MINIO_PORT="9000"
MINIO_ACCESS_KEY="your-access-key"
MINIO_SECRET_KEY="your-secret-key"

# 其他配置
NODE_ENV=production
PORT=3000
```

#### 复制部署脚本
```bash
# 将 deploy.sh 复制到部署目录
cp deploy.sh /opt/healthcare/server/
chmod +x /opt/healthcare/server/deploy.sh
```

## 使用方法

### 1. Jenkins 自动部署

#### 创建 Jenkins 项目
1. 新建 Item -> Pipeline
2. 名称: `healthcare-server`
3. Pipeline -> Definition: Pipeline script from SCM
4. SCM: Git
5. Repository URL: 你的 Git 仓库地址
6. Script Path: `server/Jenkinsfile`

#### 手动触发部署
1. 进入项目页面
2. 点击 "Build with Parameters"
3. 选择部署环境（dev/staging/prod）
4. 点击 "Build"

### 2. 手动部署

#### 构建 Docker 镜像
```bash
cd server/

# 构建镜像
docker build \
    --build-arg NODE_ENV=production \
    --build-arg BUILD_VERSION=$(git rev-parse --short HEAD) \
    -t healthcare-server:latest \
    .

# 保存镜像（用于传输到服务器）
docker save healthcare-server:latest | gzip > healthcare-server.tar.gz
```

#### 传输到服务器
```bash
# 传输镜像文件
scp healthcare-server.tar.gz user@server:/opt/healthcare/server/

# 传输环境变量文件（首次部署）
scp .env user@server:/opt/healthcare/
```

#### 在服务器上部署
```bash
# SSH 登录服务器
ssh user@server

# 执行部署
cd /opt/healthcare/server
./deploy.sh healthcare-server latest 3000 prod
```

### 3. 查看日志

```bash
# 实时查看日志
docker logs -f healthcare-server

# 查看最近 100 行日志
docker logs --tail 100 healthcare-server

# 查看特定时间后的日志
docker logs --since 2024-01-01T00:00:00 healthcare-server
```

### 4. 常用运维命令

```bash
# 查看容器状态
docker ps -f name=healthcare-server

# 进入容器
docker exec -it healthcare-server sh

# 重启服务
docker restart healthcare-server

# 停止服务
docker stop healthcare-server

# 查看容器资源使用
docker stats healthcare-server

# 查看容器详细信息
docker inspect healthcare-server
```

## 回滚操作

### 自动回滚
如果部署失败，`deploy.sh` 会自动回滚到上一个版本。

### 手动回滚
```bash
# 查看备份的版本信息
cat /opt/healthcare/backup/previous_version.txt

# 停止当前容器
docker stop healthcare-server
docker rm healthcare-server

# 启动旧版本（假设是 backup-20240101120000）
docker run -d \
    --name healthcare-server \
    --restart unless-stopped \
    -p 3000:3000 \
    -v /opt/healthcare/logs:/app/logs \
    -v /opt/healthcare/.env:/app/.env:ro \
    healthcare-server:backup-20240101120000
```

## 环境变量说明

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `NODE_ENV` | 运行环境 | `production` |
| `PORT` | 服务端口 | `3000` |
| `DATABASE_URL` | 数据库连接字符串 | - |
| `JWT_SECRET` | JWT 密钥 | - |
| `REDIS_URL` | Redis 连接字符串 | - |
| `MINIO_ENDPOINT` | MinIO 地址 | `localhost` |
| `MINIO_PORT` | MinIO 端口 | `9000` |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | - |
| `MINIO_SECRET_KEY` | MinIO 秘密密钥 | - |

## 故障排查

### 1. 容器无法启动
```bash
# 查看容器日志
docker logs healthcare-server

# 检查端口是否被占用
netstat -tuln | grep 3000

# 检查环境变量文件
cat /opt/healthcare/.env
```

### 2. 健康检查失败
```bash
# 手动测试健康检查接口
curl http://localhost:3000/api/health

# 查看容器健康状态
docker inspect --format='{{.State.Health}}' healthcare-server
```

### 3. 数据库连接失败
```bash
# 测试数据库连接
docker exec -it healthcare-server sh
npx prisma db pull
```

### 4. 内存不足
```bash
# 查看容器内存使用
docker stats healthcare-server

# 调整内存限制（修改 deploy.sh 中的 --memory 参数）
# 默认限制为 512MB
```

## 安全建议

1. **修改默认密码**: 首次部署后立即修改所有默认密码
2. **使用强密钥**: JWT_SECRET 至少 32 位随机字符串
3. **限制网络访问**: 使用防火墙限制端口访问
4. **定期更新**: 定期更新 Docker 镜像和依赖
5. **日志审计**: 定期检查访问日志和错误日志
6. **备份数据**: 定期备份数据库和重要文件
