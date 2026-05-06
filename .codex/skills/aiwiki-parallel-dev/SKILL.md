---
name: aiwiki-parallel-dev
description: Use for AI Obsidian Wiki v0.1 parallel development, splitting or executing T0-T9 tasks, coordinating multiple Codex or AI coding agents, enforcing file boundaries, worktree/branch isolation, and integration order for this repository.
---

# AIWiki Parallel Development

Use this skill only in the `llm-wiki-rag` repository.

## Required Context

Before planning or coding, read these files:

1. `docs/ai-obsidian-wiki-technical-design.md`
2. `docs/ai-obsidian-wiki-technical-design-v0.1.md`
3. `docs/ai-obsidian-wiki-v0.1-parallel-tasks.md`

Treat `docs/ai-obsidian-wiki-v0.1-parallel-tasks.md` as the source of truth for task boundaries, dependencies, branch names, merge order, and prompts.

## Operating Rules

- Work on exactly one task card at a time unless explicitly asked to orchestrate multiple tasks.
- Do not edit files outside the selected task's file boundary.
- Do not introduce v0.2 features while working on v0.1 tasks.
- Do not change shared response shape, public package structure, or migration conventions outside T0.
- Use T1 `VaultFileService` for file-system access; never let other modules use user-provided paths directly.
- Use T4 FILE block parser and frontmatter validator as the only LLM-to-file write path.
- Use T6 LLM client as the only model-call path.
- Prefer small, reviewable commits or branches named as specified in the parallel task document.

## Workflow

1. Identify the requested task: T0 through T9.
2. Read that task card fully from `docs/ai-obsidian-wiki-v0.1-parallel-tasks.md`.
3. State assumptions, dependencies, file boundary, and verification command.
4. Check the worktree for existing changes and do not overwrite unrelated user work.
5. Implement only the selected task.
6. Run the task's tests or the closest available verification.
7. Report changed files, tests run, remaining blockers, and integration dependencies.

## Orchestration Mode

When asked to coordinate multiple AI agents:

1. Use the task graph and merge order in `docs/ai-obsidian-wiki-v0.1-parallel-tasks.md`.
2. Start with T0. Do not launch other backend implementation tasks until T0 is merged or its contract files are stable.
3. Prefer parallel batches:
   - Batch 1: T0
   - Batch 2: T1, T3, T6
   - Batch 3: T2, T5
   - Batch 4: T4
   - Batch 5: T7, T8
   - Batch 6: T9
4. Give each agent the exact prompt template from section 5 of the task document.
5. Require each agent to return changed files, tests run, blockers, and contract changes.
6. Run integration review after each batch before launching dependent tasks.

## Completion Criteria

A task is not complete until:

- Its declared interfaces are implemented.
- Its required tests or verification steps have been run.
- It has not modified files outside its assigned boundary.
- It documents any cross-task dependency or contract change.
- It leaves v0.1 behavior aligned with `docs/ai-obsidian-wiki-technical-design-v0.1.md`.
