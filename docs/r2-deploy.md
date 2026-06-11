# Cloudflare R2 deploy tooling

Part 1 of terminal bucket deployment: sync local files to R2 via the AWS CLI S3-compatible API. Manifest upload and schema design are handled separately.

## Prerequisites

- [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) (`aws --version`)
- A Cloudflare account with an R2 bucket
- macOS Keychain (credentials are never stored in `.env` or committed files)

## One-time setup

### 1. Create an R2 API token

In the [Cloudflare dashboard](https://dash.cloudflare.com/):

1. Open **R2** → your bucket (or create one).
2. Go to **Manage R2 API tokens** → **Create API token**.
3. Grant **Object Read & Write** on the target bucket (or a scoped prefix).
4. Copy the **Access Key ID** and **Secret Access Key** — the secret is shown only once.

Note your **Account ID** (R2 overview page) and **bucket name**.

### 2. Store credentials in Keychain

```bash
security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-access-key-id" -w "ACCESS_KEY_ID"

security add-generic-password -U \
  -a "$USER" -s "gamelauncher-r2-secret-access-key" -w "SECRET_ACCESS_KEY"
```

Replace `ACCESS_KEY_ID` and `SECRET_ACCESS_KEY` with the values from step 1.

Verify (prints the access key id only):

```bash
source scripts/r2-from-keychain.sh
echo "R2 access key loaded (${#R2_ACCESS_KEY_ID} chars)"
```

### 3. Configure non-secret values

```bash
cp .env.example .env
```

Edit `.env` and set:

- `R2_ACCOUNT_ID` — Cloudflare account ID
- `R2_BUCKET_NAME` — target bucket name
- `R2_PUBLIC_CDN_BASE_URL` — optional public CDN base URL (no trailing slash); used later for manifest URLs

Do **not** put `R2_ACCESS_KEY_ID` or `R2_SECRET_ACCESS_KEY` in `.env`.

## Deploy

Sync a local directory to the bucket root:

```bash
./scripts/r2-deploy.sh ./path/to/local/files
```

Sync under a remote prefix:

```bash
./scripts/r2-deploy.sh ./path/to/local/files releases/v1
```

The script loads `.env` from the repo root, injects credentials from Keychain, and runs `aws s3 sync` against `https://{R2_ACCOUNT_ID}.r2.cloudflarestorage.com`.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `aws CLI not found` | Install AWS CLI v2 and ensure `aws` is on your `PATH` |
| Keychain item missing | Re-run the `security add-generic-password` commands above |
| `R2_ACCOUNT_ID is not set` | Copy `.env.example` → `.env` and set account/bucket values |
| Access denied | Confirm the API token has write access to the bucket/prefix |

## Security

- Credentials live in Keychain only — see `.cursor/skills/secret-hygiene/SKILL.md`
- `.env` is gitignored; committed config uses placeholders in `.env.example` only
- Run `scripts/scan-secrets.sh` before commit/push
