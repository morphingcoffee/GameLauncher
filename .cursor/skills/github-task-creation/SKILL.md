---
name: github-task-creation
description: >-
  Create GitHub issues and tasks for GameLauncher with project board linkage.
  Use when creating issues, tasks, roadmap items, or follow-ups for morphingcoffee/GameLauncher.
---

# GitHub Task Creation (GameLauncher)

## Project board (required for launcher work)

Every **GameLauncher-related issue** must be added to the roadmap project:

| Field | Value |
|-------|-------|
| **URL** | https://github.com/users/morphingcoffee/projects/1 |
| **Owner** | `morphingcoffee` (user) |
| **Project number** | `1` |
| **Title** | @morphingcoffee's Cross Platform Game Launcher |

Creating the issue alone is not enough — **attach it to the project before finishing**.

## Workflow

1. **Create the issue** on `morphingcoffee/GameLauncher` (`gh issue create` or GitHub MCP).
2. **Add to project #1** (see commands below).
3. **Verify** with `gh issue view <N> --json projectItems` or open the project URL.

## Add issue to project

Prefer GraphQL (works with Keychain PAT via `source tools/dev/github-pat-from-keychain.sh`):

```bash
source tools/dev/github-pat-from-keychain.sh

# Resolve issue node ID
ISSUE_ID=$(gh api graphql -f query='
  query($owner: String!, $repo: String!, $number: Int!) {
    repository(owner: $owner, name: $repo) {
      issue(number: $number) { id }
    }
  }' -f owner=morphingcoffee -f repo=GameLauncher -F number=<ISSUE_NUMBER> \
  --jq '.data.repository.issue.id')

# Add to project 1
gh api graphql -f query="
  mutation {
    addProjectV2ItemById(input: {
      projectId: \"PVT_kwHOA1fh3M4BaLRa\"
      contentId: \"$ISSUE_ID\"
    }) { item { id } }
  }"
```

Fallback if `gh project item-add` works (requires `project` scope on PAT):

```bash
gh project item-add 1 --owner morphingcoffee \
  --url https://github.com/morphingcoffee/GameLauncher/issues/<ISSUE_NUMBER>
```

If project commands fail with scope errors: `gh auth refresh -s read:project,project`.

## Issue conventions

- **Branch names:** `phase-N/short-desc` (e.g. `phase-5/settings-section`)
- **PR body:** include `Closes #N` when work completes an issue
- **Repository:** `morphingcoffee/GameLauncher` (public)

Follow `github-workflow` rule and `prompt-before-api-ops` before `gh` writes when not explicitly requested.

## Issue body template

```markdown
## Summary

<!-- What and why -->

## Success criteria

- [ ] ...

## Test plan

- [ ] ...
```

## Checklist before closing the task

- [ ] Issue created on `morphingcoffee/GameLauncher`
- [ ] Issue added to [project #1](https://github.com/users/morphingcoffee/projects/1)
- [ ] Issue URL returned to the user
