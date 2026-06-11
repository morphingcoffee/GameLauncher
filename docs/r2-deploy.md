# R2 terminal deploy

Secure upload of build artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) from the terminal. Credentials live in **macOS Keychain**; non-secret bucket settings live in a local `.env` (gitignored).

**Tooling:** [rclone](https://rclone.org/) for directory sync (retries, incremental uploads); [Wrangler](https://developers.cloudflare.com/workers/wrangler/) for Cloudflare-specific operations.

Manifest schema and `manifest.json` publishing are **out of scope** for this slice (Part 1 of [#3](https://github.com/morphingcoffee/GameLauncher/issues/3)).

## Install tools

```bash
brew install rclone wrangler
```

## One-time Cloudflare setup

1. Create an R2 bucket in the Cloudflare dashboard.
2. Create an **R2 API token** (Object Read & Write) — note the Access Key ID and Secret Access Key.
3. Create a **Cloudflare API token** with R2 permissions if you plan to use Wrangler (Account → API Tokens).
4. Copy `.env.example` to `.env` and fill in non-secret values:

```bash
cp .env.example .env
```

| Variable | Description |
|----------|-------------|
| `R2_ACCOUNT_ID` | Cloudflare account ID (dashboard URL or Overview) |
| `R2_BUCKET_NAME` | Target bucket name |
| `R2_PUBLIC_CDN_BASE_URL` | Public CDN base URL (custom domain or `r2.dev` — no auth query params) |

## Store credentials in Keychain

Never put these in `.env` or committed files.

```bash
# R2 S3-compatible keys (for rclone)
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-access-key-id" -w "YOUR_R2_ACCESS_KEY_ID"

security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "YOUR_R2_SECRET_ACCESS_KEY"

# Cloudflare API token (for wrangler)
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-wrangler-api-token" -w "YOUR_CLOUDFLARE_API_TOKEN"
```

To update an existing item, re-run with `-U` (upsert).

Load manually in a shell (optional):

```bash
source scripts/r2-from-keychain.sh
source scripts/wrangler-from-keychain.sh
```

## When to use rclone vs wrangler

| Task | Tool | Script |
|------|------|--------|
| Sync a local build directory to the bucket | **rclone** | `r2-deploy.sh sync` or `r2-sync.sh` |
| Incremental uploads, retries, large trees | **rclone** | `r2-sync.sh` |
| List buckets, bucket info, single-object get/delete | **wrangler** | `r2-wrangler.sh` |
| Create bucket, CORS, lifecycle (dashboard or IaC later) | **wrangler** / dashboard | `r2-wrangler.sh` |

**Default workflow:** run `./scripts/r2-deploy.sh sync <local-dir>` after a local build.

## Examples

Sync a directory to the bucket root:

```bash
./scripts/r2-deploy.sh sync ./path/to/artifacts
```

Sync under a prefix inside the bucket:

```bash
./scripts/r2-deploy.sh sync ./path/to/artifacts releases/v1.0.0
```

Wrangler — inspect bucket (uses Keychain token automatically):

```bash
./scripts/r2-deploy.sh wrangler r2 bucket list
./scripts/r2-deploy.sh wrangler r2 bucket info YOUR_BUCKET_NAME
```

Low-level sync (same as deploy sync subcommand):

```bash
./scripts/r2-sync.sh ./path/to/artifacts [remote-prefix]
```

## rclone retry behavior

`r2-sync.sh` passes rclone flags for transient failures:

- `--retries 5` with `--retries-sleep 5s`
- `--low-level-retries 10`

Adjust in `scripts/r2-sync.sh` if your network needs different limits.

## Security notes

- Run `scripts/scan-secrets.sh` before commit; pre-commit hooks use it automatically when configured.
- Do not commit `.env`, API keys, or account-specific URLs with auth parameters.
- See `.cursor/skills/secret-hygiene/SKILL.md` and `.cursor/skills/public-repo-safety/SKILL.md`.
