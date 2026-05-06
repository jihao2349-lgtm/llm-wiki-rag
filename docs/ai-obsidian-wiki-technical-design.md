# AI Obsidian Wiki 技术方案索引

日期：2026-05-06  
状态：版本拆分后的技术方案入口

本文件只作为技术方案入口，避免把 MVP 与后续增量能力混在同一份实现文档中。

## 版本文件

| 文件 | 范围 | 状态 |
|------|------|------|
| [ai-obsidian-wiki-technical-design-v0.1.md](./ai-obsidian-wiki-technical-design-v0.1.md) | MVP：Vault、资料导入、AI 摄入、Wiki、检索对话 | 优先实现 |
| [ai-obsidian-wiki-v0.1-parallel-tasks.md](./ai-obsidian-wiki-v0.1-parallel-tasks.md) | v0.1 并行开发任务拆分 | 分配给多个 AI 执行 |
| [ai-obsidian-wiki-technical-design-v0.2.md](./ai-obsidian-wiki-technical-design-v0.2.md) | 增量：Public RAG API、API Key、可选向量检索、多模态摄入、Chat 增强 | v0.1 稳定后实现 |

## 范围原则

- v0.1 只交付人使用的 Web Chat 场景，确保 Obsidian Vault 可读、可追溯、可安全写入。
- v0.2 在 v0.1 Service 层之上扩展外部 REST RAG API 和检索能力，不重写基础链路。
- MCP Server、Webhook、Reranker 和完整 API 审计面板统一放到 v0.3，不进入 v0.2 完成标准。
- Markdown 文件系统仍是真相来源；数据库保存索引、状态和元数据；向量库只是可关闭的增强索引。

## 实施顺序

1. 先实现并验收 v0.1。
2. 再按 v0.2 文档做数据库迁移和 Service 层增量。
3. 最后单独评审 v0.3：MCP、Webhook、Reranker、审计面板。
