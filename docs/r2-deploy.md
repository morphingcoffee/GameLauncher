# R2 terminal deploy

Secure upload of build artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) from the terminal. Credentials live in **macOS Keychain**; non-secret bucket settings live in a local `.env` (gitignored).

**Tooling:** [rclone](https://rclone.org/) for directory sync (retries, incremental uploads).

Manifest schema and `manifest.json` publishing are **out of scope** for this slice (Part 1 of [#3](https://github.com/morphingcoffee/GameLauncher/issues/3)).

## Install rclone

```bash
brew install rclone
rclone version
```

## One-time Cloudflare setup

1. Create an R2 bucket in the [Cloudflare dashboard](https://dash.cloudflare.com/).
2. Create an **R2 API token** (Object Read & Write) — note the Access Key ID and Secret Access Key.
3. Configure public access if needed (custom domain or `r2.dev` public bucket URL) in the dashboard.
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
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-access-key-id" -w "YOUR_R2_ACCESS_KEY_ID"

security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "YOUR_R2_SECRET_ACCESS_KEY"
```

To update an existing item, re-run with `-U` (upsert).

Load manually in a shell (optional):

```bash
source scripts/r2-from-keychain.sh
```

## Deploy

Sync a local build directory to the bucket:

```bash
./scripts/r2-deploy.sh ./path/to/artifacts
```

Sync under a prefix inside the bucket:

```bash
./scripts/r2-deploy.sh ./path/to/artifacts releases/v1.0.0
```

## rclone retry behavior

`r2-deploy.sh` passes rclone flags for transient failures:

- `--retries 5` with `--retries-sleep 5s`
- `--low-level-retries 10`

Adjust in `scripts/r2-deploy.sh` if your network needs different limits.

## Security notes

- Run `scripts/scan-secrets.sh` before commit; pre-commit hooks use it automatically when configured.
- Do not commit `.env`, API keys, or account-specific URLs with auth parameters.
- See `.cursor/skills/secret-hygiene/SKILL.md` and `.cursor/skills/public-repo-safety/SKILL.md`.
