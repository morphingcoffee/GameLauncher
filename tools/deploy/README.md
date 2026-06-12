# R2 terminal deploy

Upload artifacts to [Cloudflare R2](https://developers.cloudflare.com/r2/) via [rclone](https://rclone.org/). Credentials in **macOS Keychain**; bucket settings in local `.env` (gitignored).

Manifest schema is out of scope for this slice ([#3](https://github.com/morphingcoffee/GameLauncher/issues/3)).

## Setup

```bash
brew install rclone
cp .env.example .env   # R2_ACCOUNT_ID, R2_BUCKET_NAME, R2_PUBLIC_CDN_BASE_URL
```

1. Create an R2 bucket and enable public access (`r2.dev` or custom domain) if the app will fetch over HTTPS.
2. **R2 → Manage R2 API Tokens → Create** — **Object Read & Write**, scoped to your bucket. Copy the **S3** Access Key ID and Secret Access Key (not the Cloudflare API “Token value”).
3. Store keys in Keychain:

```bash
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-access-key-id" -w "ACCESS_KEY_ID"
security add-generic-password -U -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "SECRET_ACCESS_KEY"
```

Re-run with `-U` after rotating a token; update **both** items.

## Test and deploy

```bash
./tools/deploy/r2-test-auth.sh
./tools/deploy/r2-deploy.sh ./path/to/artifacts
./tools/deploy/r2-deploy.sh ./path/to/artifacts releases/v1.0.0   # optional prefix
```

`r2-deploy.sh` runs a dry-run first. **Sync deletes** remote files under the target prefix that are not in your local directory — use a dedicated prefix (e.g. `releases/v1.0.0`). If the dry-run reports deletes, review the list and pass `--allow-deletes` to proceed.

## Security

- Run `tools/dev/scan-secrets.sh` before commit.
- See `.cursor/skills/secret-hygiene/SKILL.md`.
