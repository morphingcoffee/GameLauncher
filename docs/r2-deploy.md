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
2. Create an **R2 API token** (Object Read & Write, scoped to your bucket) — note the Access Key ID and Secret Access Key. You do **not** need Admin Read & Write; `r2-deploy.sh` sets `no_check_bucket = true` because scoped tokens cannot create or list buckets (a common rclone + R2 403 cause).
3. Confirm the token’s **Access Key ID** and **Secret** are from **R2 → Manage R2 API tokens** (S3-compatible), not a global Cloudflare API token.
4. Configure public access if needed (custom domain or `r2.dev` public bucket URL) in the dashboard.
5. Copy `.env.example` to `.env` and fill in non-secret values:

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

Paste keys exactly as shown once when creating the token — the secret is only shown once. If you recreate the token, update **both** Keychain items.

### `SignatureDoesNotMatch`

rclone reached R2 but the access key / secret pair does not match. Usually:

1. Secret in Keychain is from an old token (recreate token → update Keychain).
2. Access key and secret were swapped or copied with extra whitespace.
3. Keys are from a different Cloudflare account than `R2_ACCOUNT_ID` in `.env`.

Check lengths without printing secrets:

```bash
security find-generic-password -a "$USER" -s gamelauncher-r2-access-key-id -w | wc -c   # expect 33 (32 + newline) or 32
security find-generic-password -a "$USER" -s gamelauncher-r2-secret-access-key -w | wc -c  # typically 65 (64 + newline)
```

Test credentials:

```bash
./scripts/r2-test-auth.sh
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
