# AI Obsidian Wiki Frontend

Vue 3 + Vite mock frontend for the AI Obsidian Wiki PRD. The current build uses local mock data so the product flow can be reviewed before backend APIs are connected.

## Features

- Dashboard for Vault status, source counts, Wiki pages, index health, and review queue.
- Sources page for file upload, URL import, parse preview, and manual AI ingest entry.
- Tasks page for serial ingest queue, retries, errors, and written file tracking.
- Chat page with Vault-based answer mock, citations, and save-to-Wiki action.
- Wiki page with generated Markdown tree, frontmatter, and Obsidian-compatible preview.
- Settings page for Vault, OpenAI-compatible LLM, context, embedding, and safety switches.

## Development

```bash
npm install
npm run dev
```

## Verification

```bash
npm run typecheck
npm run build
```

## Project Structure

```text
src/
  components/    shared layout, icons, status and metric UI
  pages/         PRD pages: Dashboard, Sources, Tasks, Chat, Wiki, Settings
  mock-data.ts   local mock state for product walkthrough
  types.ts       frontend domain types
  utils/         small shared helpers
```

## Backend Handoff

Replace `src/mock-data.ts` with API calls once the backend is ready. The mock shape follows the PRD models: `vault_project`, `source_document`, `wiki_page`, `ingest_task`, `chat_session`, and `chat_message`.
