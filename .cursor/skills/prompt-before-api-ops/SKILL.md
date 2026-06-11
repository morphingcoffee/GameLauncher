---
name: prompt-before-api-ops
description: >-
  Ask before git push, Bugbot/subagents, and other costly Cursor or network
  operations. Use when about to push, spawn Task agents, run review-bugbot,
  WebSearch, GitHub MCP/gh, or browser automation. Bugbot on PRs is manual-only
  (user comments bugbot run).
---

# Prompt Before API Ops

## Rule

**Ask before gated operations.** Wait for explicit approval in the current thread.

Bugbot on GitHub PRs is **manual-only** (user triggers via `bugbot run`). Do not assume a push should trigger review.

## Ask first

| Priority | Operation |
|----------|-----------|
| High | `git push`, `git push --force` |
| High | `Task` subagents, `review-bugbot` / `bugbot` subagent |
| Medium | `WebSearch`, `WebFetch`, GitHub MCP, `gh` (writes), browser MCP |
| Medium | Network deploy (`r2-deploy.sh`), `curl` to external APIs |

Mention when relevant: push triggers CI; subagents multiply token usage.

## Proceed without asking

Local file read/edit, grep/search, `git status` / `diff` / `log`, local builds/tests.

## Workflow

1. State what you want to run and why.
2. Use `AskQuestion` when available; otherwise wait for a reply.
3. Exemption: user explicitly requested it in the same message ("push this", "run bugbot").

## Related

- `prompt-before-tooling` — which external CLI to adopt
- `secret-hygiene` — credentials
- `github-workflow` — PR/issue conventions
