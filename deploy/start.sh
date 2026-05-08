#!/usr/bin/env bash
# ============================================================
# AI Wiki - 生产部署脚本
# 用法:
#   首次部署:  bash start.sh
#   更新代码:  bash start.sh update
#   停止服务:  bash start.sh stop
#   查看日志:  bash start.sh logs [服务名]
#   服务状态:  bash start.sh status
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.prod.yml"
ENV_FILE="${SCRIPT_DIR}/.env"
ENV_EXAMPLE="${SCRIPT_DIR}/.env.example"

cd "${SCRIPT_DIR}"

# ---------- 颜色输出 ----------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ---------- 检查依赖 ----------
check_deps() {
  command -v docker >/dev/null 2>&1 || error "docker 未安装"
  docker compose version >/dev/null 2>&1 || error "docker compose 插件未安装（需要 v2）"
}

# ---------- 检查 .env ----------
check_env() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    warn ".env 不存在，正在从模板复制..."
    cp "${ENV_EXAMPLE}" "${ENV_FILE}"
    warn "请先编辑 ${ENV_FILE} 填写真实配置，然后重新运行此脚本"
    exit 0
  fi

  # 检查必填项
  local missing=0
  for key in MYSQL_ROOT_PASSWORD VITE_API_BASE_URL OBSIDIAN_VAULT_PATH; do
    local val
    val=$(grep -E "^${key}=" "${ENV_FILE}" 2>/dev/null | cut -d= -f2- | tr -d '[:space:]') || val=""
    if [[ -z "${val}" || "${val}" == "请修改为强密码" ]]; then
      warn "请在 .env 中设置: ${key}"
      missing=$((missing + 1))
    fi
  done
  [[ ${missing} -gt 0 ]] && error "请先完善 .env 配置"
}

# ---------- 确保 Obsidian 目录存在 ----------
ensure_obsidian_dir() {
  local vault_path
  vault_path=$(grep -E "^OBSIDIAN_VAULT_PATH=" "${ENV_FILE}" 2>/dev/null | cut -d= -f2- | tr -d '[:space:]') || vault_path=""
  if [[ -n "${vault_path}" && ! -d "${vault_path}" ]]; then
    info "创建 Obsidian Vault 目录: ${vault_path}"
    mkdir -p "${vault_path}"
  fi
}

# ---------- 构建并启动 ----------
cmd_start() {
  info "=== 构建镜像（增量构建，首次较慢）==="
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" build --parallel

  info "=== 启动所有服务 ==="
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d

  info "=== 等待后端健康检查 ==="
  local max_wait=120
  local elapsed=0
  until docker exec aiwiki-backend wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; do
    sleep 3
    elapsed=$((elapsed + 3))
    if [[ ${elapsed} -ge ${max_wait} ]]; then
      warn "后端健康检查超时，请查看日志: bash start.sh logs backend"
      break
    fi
    echo -n "."
  done
  echo ""

  info "=== 部署完成 ==="
  echo ""
  echo "  后端 API : http://172.17.18.87:8090"
  echo "  前端页面 : http://172.17.18.87:4001"
  echo "  公网访问 : http://39.97.248.52  (经 Nginx 代理)"
  echo ""
  echo "  数据库迁移由 Flyway 在后端启动时自动执行（增量）"
  echo ""
}

# ---------- 增量更新（不重建基础设施容器）----------
cmd_update() {
  info "=== 拉取最新代码 ==="
  cd "${PROJECT_ROOT}"
  git pull

  info "=== 重新构建应用镜像 ==="
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" build --parallel backend frontend

  info "=== 滚动重启应用容器（不影响 MySQL/Redis）==="
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --no-deps backend frontend

  info "=== 更新完成，Flyway 将自动执行新增迁移脚本 ==="
}

# ---------- 停止 ----------
cmd_stop() {
  info "停止所有容器（数据不删除）"
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" stop
}

# ---------- 日志 ----------
cmd_logs() {
  local svc="${1:-}"
  if [[ -n "${svc}" ]]; then
    docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs -f --tail=200 "${svc}"
  else
    docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs -f --tail=100
  fi
}

# ---------- 状态 ----------
cmd_status() {
  docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps
}

# ============================================================
# 入口
# ============================================================
check_deps

case "${1:-start}" in
  start)
    check_env
    ensure_obsidian_dir
    cmd_start
    ;;
  update)
    check_env
    cmd_update
    ;;
  stop)
    cmd_stop
    ;;
  logs)
    cmd_logs "${2:-}"
    ;;
  status)
    cmd_status
    ;;
  *)
    echo "用法: bash start.sh [start|update|stop|logs [服务名]|status]"
    exit 1
    ;;
esac
