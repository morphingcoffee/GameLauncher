---
name: github-task-creation
description: >-
  Create GitHub issues and tasks for GameLauncher with project board linkage.
  Use when creating issues, tasks, roadmap items, follow-ups, starting
  implementation on an issue (move Status to In progress), opening a PR (In
  review), or merging (Done) on the morphingcoffee/GameLauncher roadmap project.
---

# GitHub Task Creation (GameLauncher)

## Mandatory board lifecycle — do this every time

**Board Status updates are required workflow steps, not optional polish.**

| When | Status | Timing |
|------|--------|--------|
| User asks to work on issue **#N** (`fix #N`, `implement #N`, `let's do #N`, etc.) | **`In progress`** | **First action** — before reading code, branching, or editing files |
| PR opened for **#N** (`Closes #N`) | **`In review`** | **Immediately after** `gh pr create` succeeds |
| PR merged for **#N** | **`Done`** | After merge is confirmed |
| New issue created, not started | **`Backlog`** | When adding to project #1 |

**Hard rules:**

- **Never** leave an issue in `Backlog` while you are actively working on it.
- **Never** leave an issue in `In progress` after you open a PR — it must be `In review`.
- **Never** skip a Status update because `prompt-before-api-ops` — board writes are exempt (see that skill).
- **Never** post implementation-status comments on issues — the board Status field is the source of truth.

### Agent checklist for “work on #N”

1. Run `./tools/dev/github-project-status.sh N "In progress"` — **before anything else**
2. Implement (branch, code, commit)
3. Open PR with `Closes #N`
4. Run `./tools/dev/github-project-status.sh N "In review"` — **immediately after PR is created**
5. After merge → `./tools/dev/github-project-status.sh N Done`

If you notice you skipped step 1 or 4, run the missing update **immediately** — do not wait to be asked.

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

## Set Status — preferred command

```bash
./tools/dev/github-project-status.sh <ISSUE_NUMBER> "<STATUS>"
```

Examples:

```bash
./tools/dev/github-project-status.sh 44 "In progress"   # user asked to fix #44
./tools/dev/github-project-status.sh 44 "In review"   # PR opened with Closes #44
./tools/dev/github-project-status.sh 44 Done          # PR merged
```

The script sources the Keychain PAT, adds the issue to project #1 if missing, and sets Status. Requires `read:project` and `project` scopes:

```bash
gh auth refresh -s read:project,project
```

Prefer Keychain PAT when available: `source tools/dev/github-pat-from-keychain.sh`

## Status option IDs (manual GraphQL fallback)

| Status | `singleSelectOptionId` |
|--------|------------------------|
| Backlog | `f75ad846` |
| In progress | `47fc9ee4` |
| In review | `df73e18b` |
| Done | `98236657` |

`projectId`: `PVT_kwHOA1fh3M4BaLRa` · Status `fieldId`: `PVTSSF_lAHOA1fh3M4BaLRazhVEtAA`

### Resolve project item ID from the issue (do not paginate the whole board)

```bash
ITEM_ID=$(gh api graphql -f query='
  query($owner: String!, $repo: String!, $number: Int!) {
    repository(owner: $owner, name: $repo) {
      issue(number: $number) {
        projectItems(first: 20) {
          nodes {
            id
            project { number }
          }
        }
      }
    }
  }' -f owner=morphingcoffee -f repo=GameLauncher -F number=<ISSUE_NUMBER> \
  --jq '.data.repository.issue.projectItems.nodes[]
    | select(.project.number == 1)
    | .id' | head -1)
```

If empty, add to project first:

```bash
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

### Verify

```bash
gh issue view <N> --repo morphingcoffee/GameLauncher --json projectItems
```

Confirm `projectItems` is non-empty and Status matches intent on the [project board](https://github.com/users/morphingcoffee/projects/1).

## Implement existing issue workflow

1. Identify issue **#N** (user link, branch context, or `Closes #N` in PR).
2. **`./tools/dev/github-project-status.sh N "In progress"`** — mandatory first step.
3. Create branch `phase-N/short-desc`, implement, commit.
4. Open PR with `Closes #N`.
5. **`./tools/dev/github-project-status.sh N "In review"`** — mandatory immediately after PR creation.
6. After merge → **`./tools/dev/github-project-status.sh N Done`**.

## Create issue workflow

1. **Create the issue** on `morphingcoffee/GameLauncher` (`gh issue create` or GitHub MCP).
2. **Add to project #1** with Status `Backlog` (`github-project-status.sh N Backlog`).
3. **Verify** with `gh issue view <N> --json projectItems`.

## Issue conventions

- **Branch names:** `phase-N/short-desc` (e.g. `phase-5/settings-section`)
- **PR body:** include `Closes #N` when work completes an issue
- **On PR merge:** set project Status to `Done` (do not comment on the issue)
- **On implementation start:** set project Status to `In progress` — **first action, mandatory**
- **On PR open:** set project Status to `In review` — **mandatory, same turn as PR creation**
- **Repository:** `morphingcoffee/GameLauncher` (public)

Board Status writes (`github-project-status.sh`, project field mutations) proceed **without asking** per `prompt-before-api-ops`. Ask before unrelated `gh` writes (new issues, comments, etc.) when the user did not request them.

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
- [ ] Status moved to `In progress` **before** implementation when user asked to work on the issue
- [ ] Status moved to `In review` **immediately after** PR open
- [ ] Issue URL returned to the user
- [ ] No status/progress comment posted on the issue
