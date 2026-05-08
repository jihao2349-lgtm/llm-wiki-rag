# llm-wiki-rag 项目说明

## 启动方式

### 后端 + 基础设施（Docker）
```bash
cd /Users/jihao/Public/work-space/llm-wiki-rag
docker compose up -d          # 启动 MySQL / Redis / Spring Boot 后端
docker compose up --build -d  # 代码有变更时重新 build
```

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MySQL | localhost:3306 |
| Redis | localhost:6379 |

### 前端（Vite 开发服务器）
```bash
cd frontend
# 首次或依赖变更时
npm install
# 启动开发服务器
npm run dev
```

前端运行在 **http://127.0.0.1:5173**（Vite 默认端口），热更新。

> 注意：不要用 Docker 的前端容器（port 3000）做开发，那是生产静态打包版本。

### frontend/.env 配置
```
VITE_APP_NAME=AI Obsidian Wiki
VITE_API_BASE_URL=http://localhost:8080
```

## 项目结构

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot 3.x + Java 21 后端 |
| `frontend/` | Vue 3 + Vite 管理面板 |
| `llm_wiki/` | Tauri 桌面应用（独立项目） |
| `docs/` | 产品文档与技术设计 |
| `docker-compose.yml` | 生产/集成部署配置 |

## 首次启动 / 重建容器后必做

Docker 容器重建后 MySQL 是新的空库，**必须先初始化 Vault**，否则所有 API 返回 `vault not initialized`：

```bash
curl -s -X POST "http://localhost:8080/api/vault/init" \
  -H "Content-Type: application/json" \
  -d '{"name":"Personal AI Notes","path":"/obsidian","purpose":"个人知识库"}'
```

验证是否成功：
```bash
curl -s "http://localhost:8080/api/dashboard/overview?vaultId=1"
# 返回 code:200 即正常
```

> 原因：`docker compose down`（不加 `-v`）会保留旧 volume，但换了 compose 项目名（如从 worktree 切回主目录）会生成新 volume，旧数据不会自动迁移。

## 重要约定

- docker compose 必须从主项目目录（`llm-wiki-rag/`）执行，不要从 worktree 启动
- 前端开发地址：**http://127.0.0.1:5173**（Vite dev server）
- 后端地址：**http://localhost:8080**
