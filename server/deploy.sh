#!/bin/bash
# ==========================================
# 健康管理服务端部署脚本
# 用法: ./deploy.sh <镜像名> <版本> <端口> <环境>
# ==========================================

set -e  # 遇到错误立即退出

# 参数解析
IMAGE_NAME=${1:-"healthcare-server"}
BUILD_VERSION=${2:-"latest"}
APP_PORT=${3:-"3000"}
DEPLOY_ENV=${4:-"dev"}

# 配置
CONTAINER_NAME="healthcare-server"
DEPLOY_PATH="/opt/healthcare/server"
LOG_PATH="/opt/healthcare/logs"
BACKUP_PATH="/opt/healthcare/backup"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 创建必要的目录
create_directories() {
    log_info "创建部署目录..."
    mkdir -p ${DEPLOY_PATH}
    mkdir -p ${LOG_PATH}
    mkdir -p ${BACKUP_PATH}
}

# 加载 Docker 镜像
load_image() {
    log_info "加载 Docker 镜像..."
    IMAGE_FILE="${IMAGE_NAME}.tar.gz"

    if [ -f "${DEPLOY_PATH}/${IMAGE_FILE}" ]; then
        docker load < "${DEPLOY_PATH}/${IMAGE_FILE}"
        rm -f "${DEPLOY_PATH}/${IMAGE_FILE}"
        log_info "镜像加载成功: ${IMAGE_NAME}:${BUILD_VERSION}"
    else
        log_warn "镜像文件不存在，尝试从仓库拉取..."
        docker pull ${IMAGE_NAME}:${BUILD_VERSION} || {
            log_error "镜像拉取失败"
            exit 1
        }
    fi
}

# 备份当前运行的容器
backup_current() {
    log_info "备份当前版本..."

    if docker ps -q -f name=${CONTAINER_NAME} | grep -q .; then
        CURRENT_IMAGE=$(docker inspect --format='{{.Config.Image}}' ${CONTAINER_NAME})
        BACKUP_TAG="backup-$(date +%Y%m%d%H%M%S)"

        # 保存当前镜像标签
        echo "${CURRENT_IMAGE}" > ${BACKUP_PATH}/previous_version.txt

        log_info "当前版本已备份: ${CURRENT_IMAGE}"
    else
        log_warn "没有找到运行中的容器，跳过备份"
    fi
}

# 停止当前容器
stop_current() {
    log_info "停止当前容器..."

    if docker ps -q -f name=${CONTAINER_NAME} | grep -q .; then
        docker stop ${CONTAINER_NAME} --time 30
        log_info "容器已停止"
    fi

    if docker ps -aq -f name=${CONTAINER_NAME} | grep -q .; then
        docker rm ${CONTAINER_NAME}
        log_info "容器已删除"
    fi
}

# 启动新容器
start_new() {
    log_info "启动新容器..."

    # 根据环境设置不同的环境变量
    case ${DEPLOY_ENV} in
        prod)
            NODE_ENV="production"
            LOG_LEVEL="info"
            ;;
        staging)
            NODE_ENV="staging"
            LOG_LEVEL="debug"
            ;;
        dev)
            NODE_ENV="development"
            LOG_LEVEL="debug"
            ;;
        *)
            NODE_ENV="production"
            LOG_LEVEL="info"
            ;;
    esac

    # 启动容器
    docker run -d \
        --name ${CONTAINER_NAME} \
        --restart unless-stopped \
        -p ${APP_PORT}:3000 \
        -v ${LOG_PATH}:/app/logs \
        -v /opt/healthcare/.env:/app/.env:ro \
        -e NODE_ENV=${NODE_ENV} \
        -e LOG_LEVEL=${LOG_LEVEL} \
        -e TZ=Asia/Shanghai \
        --health-cmd "curl -f http://localhost:3000/api/health || exit 1" \
        --health-interval 30s \
        --health-timeout 10s \
        --health-start-period 30s \
        --health-retries 3 \
        --memory 512m \
        --cpus 1 \
        ${IMAGE_NAME}:${BUILD_VERSION}

    log_info "容器启动命令已执行"
}

# 等待服务就绪
wait_for_ready() {
    log_info "等待服务就绪..."

    MAX_RETRIES=30
    RETRY_INTERVAL=2
    RETRY_COUNT=0

    while [ ${RETRY_COUNT} -lt ${MAX_RETRIES} ]; do
        if curl -f -s http://localhost:${APP_PORT}/api/health > /dev/null 2>&1; then
            log_info "✅ 服务已就绪！"
            return 0
        fi

        RETRY_COUNT=$((RETRY_COUNT + 1))
        log_warn "等待服务启动... (${RETRY_COUNT}/${MAX_RETRIES})"
        sleep ${RETRY_INTERVAL}
    done

    log_error "❌ 服务启动超时"
    return 1
}

# 验证部署
verify_deployment() {
    log_info "验证部署..."

    # 检查容器状态
    CONTAINER_STATUS=$(docker inspect --format='{{.State.Status}}' ${CONTAINER_NAME})
    if [ "${CONTAINER_STATUS}" != "running" ]; then
        log_error "容器状态异常: ${CONTAINER_STATUS}"
        return 1
    fi

    # 检查健康状态
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' ${CONTAINER_NAME})
    if [ "${HEALTH_STATUS}" != "healthy" ]; then
        log_warn "容器健康检查状态: ${HEALTH_STATUS}"
    fi

    # 检查端口是否监听
    if ! netstat -tuln | grep -q ":${APP_PORT}"; then
        log_warn "端口 ${APP_PORT} 未监听"
    fi

    log_info "部署验证完成"
    return 0
}

# 回滚到上一个版本
rollback() {
    log_error "执行回滚..."

    if [ -f "${BACKUP_PATH}/previous_version.txt" ]; then
        PREVIOUS_IMAGE=$(cat ${BACKUP_PATH}/previous_version.txt)
        log_info "回滚到版本: ${PREVIOUS_IMAGE}"

        stop_current

        docker run -d \
            --name ${CONTAINER_NAME} \
            --restart unless-stopped \
            -p ${APP_PORT}:3000 \
            -v ${LOG_PATH}:/app/logs \
            -v /opt/healthcare/.env:/app/.env:ro \
            -e NODE_ENV=production \
            -e TZ=Asia/Shanghai \
            ${PREVIOUS_IMAGE}

        if wait_for_ready; then
            log_info "✅ 回滚成功"
        else
            log_error "❌ 回滚失败，需要人工介入"
            exit 1
        fi
    else
        log_error "没有找到可回滚的版本"
        exit 1
    fi
}

# 清理旧镜像
cleanup() {
    log_info "清理旧镜像..."

    # 删除悬空镜像
    docker image prune -f

    # 保留最近 3 个版本的镜像
    docker images ${IMAGE_NAME} --format "{{.Tag}}" | \
        grep -v "latest" | \
        sort -r | \
        tail -n +4 | \
        xargs -I {} docker rmi ${IMAGE_NAME}:{} 2>/dev/null || true

    log_info "清理完成"
}

# 输出部署信息
print_deployment_info() {
    echo ""
    echo "=========================================="
    echo "✅ 部署完成！"
    echo "=========================================="
    echo "镜像: ${IMAGE_NAME}:${BUILD_VERSION}"
    echo "环境: ${DEPLOY_ENV}"
    echo "端口: ${APP_PORT}"
    echo "容器: ${CONTAINER_NAME}"
    echo ""
    echo "访问地址:"
    echo "  - API: http://localhost:${APP_PORT}"
    echo "  - 文档: http://localhost:${APP_PORT}/api/docs"
    echo ""
    echo "常用命令:"
    echo "  - 查看日志: docker logs -f ${CONTAINER_NAME}"
    echo "  - 进入容器: docker exec -it ${CONTAINER_NAME} sh"
    echo "  - 重启服务: docker restart ${CONTAINER_NAME}"
    echo "  - 停止服务: docker stop ${CONTAINER_NAME}"
    echo "=========================================="
}

# 主函数
main() {
    echo ""
    echo "=========================================="
    echo "健康管理系统 - 服务端部署"
    echo "=========================================="
    echo "镜像: ${IMAGE_NAME}:${BUILD_VERSION}"
    echo "环境: ${DEPLOY_ENV}"
    echo "端口: ${APP_PORT}"
    echo "=========================================="
    echo ""

    # 执行部署流程
    create_directories
    load_image
    backup_current
    stop_current
    start_new

    # 等待服务就绪并验证
    if wait_for_ready && verify_deployment; then
        cleanup
        print_deployment_info
    else
        log_error "部署失败，开始回滚..."
        rollback
    fi
}

# 执行主函数
main
