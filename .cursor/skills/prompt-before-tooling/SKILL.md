---
name: prompt-before-tooling
description: >-
  Requires user approval before choosing external tools, CLIs, cloud SDKs,
  deployment methods, or new infra dependencies. Use when implementing CDN/R2/S3
  deploy, CI tooling, package managers for ops scripts, or any workflow where
  multiple tooling options exist (e.g. AWS CLI vs wrangler vs rclone).
---

# Prompt Before Tooling

## Rule

**Never adopt, install, document, or commit an external tool or service integration without explicit user approval.**

This applies whenever more than one reasonable option exists — even if one option is common or you have a strong preference.

## What counts as a tooling decision

| Category | Examples |
|----------|----------|
| CLI / ops | AWS CLI, `wrangler`, `rclone`, `s5cmd`, `mc` |
| Cloud APIs | S3-compatible sync, Cloudflare API, Terraform |
| Runtime deps | New Gradle plugins, npm/pip packages for deploy scripts |
| CI actions | Third-party GitHub Actions vs shell scripts |
| Auth patterns | Keychain vs `.env` vs OIDC (secret storage is separate — see `secret-hygiene`) |

## Required workflow

1. **Stop** before writing scripts, docs, or config that assume a specific tool.
2. **Present 2–4 options** with one-line tradeoffs each (setup cost, deps, fit for this repo).
3. **Use `AskQuestion`** when the tool is available; otherwise ask in chat and wait for a reply.
4. **Implement only after** the user picks an option or gives explicit direction.

## Do not

- Pick a tool because it is "standard" or "what I used last time"
- Bake a CLI into scripts/docs/PRs and mention alternatives only in passing
- Proceed on infra tasks with "I'll use X unless you prefer otherwise"

## Do

- Propose options first, then implement the chosen path
- If the user already named a tool in the same thread, that counts as approval for that choice only
- Re-prompt when the task shifts (e.g. R2 upload approved via AWS CLI does not approve CI or app networking tools)

## Example (R2 deploy)

**Wrong:** Add `scripts/r2-deploy.sh` using `aws s3 sync` without asking.

**Right:**

> For R2 uploads from the terminal, which approach do you want?
> - **AWS CLI** (`aws s3 sync`) — S3-compatible, widely documented
> - **Wrangler** — Cloudflare-native, needs Wrangler login
> - **rclone** — flexible, extra config file
>
> I won't add deploy scripts until you choose.

## Related skills

- `secret-hygiene` — where credentials live after tooling is chosen
- `public-repo-safety` — no secrets or machine paths in committed files
