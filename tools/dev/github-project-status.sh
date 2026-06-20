#!/usr/bin/env bash
# Set roadmap project #1 Status for a GameLauncher issue.
# Usage: github-project-status.sh <issue-number> <Backlog|In progress|In review|Done>
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=/dev/null
source "$ROOT/tools/dev/github-pat-from-keychain.sh" 2>/dev/null || true

ISSUE_NUMBER="${1:?issue number required}"
STATUS_NAME="${2:?status required — Backlog | In progress | In review | Done}"

PROJECT_ID="PVT_kwHOA1fh3M4BaLRa"
STATUS_FIELD_ID="PVTSSF_lAHOA1fh3M4BaLRazhVEtAA"

case "$STATUS_NAME" in
  Backlog) OPTION_ID="f75ad846" ;;
  "In progress") OPTION_ID="47fc9ee4" ;;
  "In review") OPTION_ID="df73e18b" ;;
  Done) OPTION_ID="98236657" ;;
  *)
    echo "Unknown status: $STATUS_NAME" >&2
    exit 1
    ;;
esac

ISSUE_ID="$(
  gh api graphql -f query='
    query($owner: String!, $repo: String!, $number: Int!) {
      repository(owner: $owner, name: $repo) {
        issue(number: $number) { id }
      }
    }' -f owner=morphingcoffee -f repo=GameLauncher -F number="$ISSUE_NUMBER" \
    --jq '.data.repository.issue.id'
)"

ITEM_ID="$(
  gh api graphql -f query='
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
    }' -f owner=morphingcoffee -f repo=GameLauncher -F number="$ISSUE_NUMBER" \
    --jq '.data.repository.issue.projectItems.nodes[]
      | select(.project.number == 1)
      | .id' | head -1
)"

if [[ -z "$ITEM_ID" ]]; then
  ITEM_ID="$(
    gh api graphql -f query="
      mutation {
        addProjectV2ItemById(input: {
          projectId: \"$PROJECT_ID\"
          contentId: \"$ISSUE_ID\"
        }) { item { id } }
      }" --jq '.data.addProjectV2ItemById.item.id'
  )"
fi

gh api graphql -f query="
  mutation {
    updateProjectV2ItemFieldValue(input: {
      projectId: \"$PROJECT_ID\"
      itemId: \"$ITEM_ID\"
      fieldId: \"$STATUS_FIELD_ID\"
      value: { singleSelectOptionId: \"$OPTION_ID\" }
    }) { projectV2Item { id } }
  }" >/dev/null

echo "Issue #$ISSUE_NUMBER → $STATUS_NAME"
