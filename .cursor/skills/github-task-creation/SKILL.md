---
name: github-task-creation
description: >-
  Create GitHub issues and tasks for GameLauncher with project board linkage.
  Use when creating issues, tasks, roadmap items, follow-ups, or updating task
  status on the morphingcoffee/GameLauncher roadmap project.
---

# GitHub Task Creation (GameLauncher)

## Project board (required for launcher work)

Every **GameLauncher-related issue** must be on the roadmap project:

| Field | Value |
|-------|-------|
| **URL** | https://github.com/users/morphingcoffee/projects/1 |
| **Owner** | `morphingcoffee` (user) |
| **Project number** | `1` |
| **Project ID** | `PVT_kwHOA1fh3M4BaLRa` |
| **Title** | @morphingcoffee's Cross Platform Game Launcher |

Creating the issue alone is not enough — **attach it to the project before finishing**.

## Status updates — project board only

**Do not post implementation-status comments on issues.** Progress is tracked on the project board **Status** field (column), not in issue comments or issue-body edits.

| Work state | Set Status to |
|------------|---------------|
| Issue created, not started | `Backlog` |
| Implementation in progress (branch/WIP) | `In progress` |
| PR open / awaiting review | `In review` |
| PR merged / work complete | `Done` |

When the user asks to update task status, **only** add/move the issue on project #1 and set **Status**. Do not comment on the issue with checklists or progress summaries.

Requires `read:project` and `project` scopes on the GitHub token. If missing:

```bash
gh auth refresh -s read:project,project
```

Prefer Keychain PAT when available: `source tools/dev/github-pat-from-keychain.sh`

### 1. Add issue to project (if not already on board)

```bash
source tools/dev/github-pat-from-keychain.sh

ISSUE_ID=$(gh api graphql -f query='
  query($owner: String!, $repo: String!, $number: Int!) {
    repository(owner: $owner, name: $repo) {
      issue(number: $number) { id }
    }
  }' -f owner=morphingcoffee -f repo=GameLauncher -F number=<ISSUE_NUMBER> \
  --jq '.data.repository.issue.id')

gh api graphql -f query="
  mutation {
    addProjectV2ItemById(input: {
      projectId: \"PVT_kwHOA1fh3M4BaLRa\"
      contentId: \"$ISSUE_ID\"
    }) { item { id } }
  }"
```

Fallback: `gh project item-add 1 --owner morphingcoffee --url https://github.com/morphingcoffee/GameLauncher/issues/<N>`

### 2. Set Status field

Query Status field and option IDs (run once per session or when options change):

```bash
gh api graphql -f query='
  query {
    user(login: "morphingcoffee") {
      projectV2(number: 1) {
        field(name: "Status") {
          ... on ProjectV2SingleSelectField {
            id
            options { id name }
          }
        }
      }
    }
  }' --jq '.data.user.projectV2.field'
```

Resolve the project **item** ID for the issue:

```bash
ITEM_ID=$(gh api graphql -f query='
  query($owner: String!, $number: Int!) {
    user(login: $owner) {
      projectV2(number: 1) {
        items(first: 100) {
          nodes {
            id
            content {
              ... on Issue { number }
            }
          }
        }
      }
    }
  }' -f owner=morphingcoffee -F number=<ISSUE_NUMBER> \
  --jq '.data.user.projectV2.items.nodes[] | select(.content.number == <ISSUE_NUMBER>) | .id')
```

Update Status (substitute `STATUS_FIELD_ID`, `OPTION_ID`, `ITEM_ID` from queries above):

```bash
gh api graphql -f query="
  mutation {
    updateProjectV2ItemFieldValue(input: {
      projectId: \"PVT_kwHOA1fh3M4BaLRa\"
      itemId: \"$ITEM_ID\"
      fieldId: \"$STATUS_FIELD_ID\"
      value: { singleSelectOptionId: \"$OPTION_ID\" }
    }) {
      projectV2Item { id }
    }
  }"
```

### 3. Verify

```bash
gh issue view <N> --repo morphingcoffee/GameLauncher --json projectItems
```

Confirm `projectItems` is non-empty and Status matches intent on the [project board](https://github.com/users/morphingcoffee/projects/1).

## Create issue workflow

1. **Create the issue** on `morphingcoffee/GameLauncher` (`gh issue create` or GitHub MCP).
2. **Add to project #1** with Status `Backlog`.
3. **Verify** with `gh issue view <N> --json projectItems`.

## Issue conventions

- **Branch names:** `phase-N/short-desc` (e.g. `phase-5/settings-section`)
- **PR body:** include `Closes #N` when work completes an issue
- **On PR merge:** set project Status to `Done` (do not comment on the issue)
- **On implementation start:** set project Status to `In progress`
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
- [ ] Project **Status** set correctly (`Backlog` / `In progress` / `In review` / `Done`)
- [ ] Issue URL returned to the user
- [ ] No status/progress comment posted on the issue
