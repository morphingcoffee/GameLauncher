---
name: public-repo-safety
description: >-
  Prevents local machine identity and sensitive paths from leaking into the
  public GameLauncher repository. Use when editing .gitignore, config files,
  docs, CI workflows, or any file that might contain local paths or sensitive
  values.
---

# Public Repo Safety

`morphingcoffee/GameLauncher` is **public**. Treat every committed file as world-readable.

## Never commit

| Category | Examples |
|----------|----------|
| macOS user paths | home-dir paths with your username (e.g. under macOS `Users` folder) |
| Windows profile paths | `C:\Users\alice\...` |
| Hostnames / IPs | `morphingcoffee-macbook.local`, `192.168.x.x` |
| IDE machine state | `.idea/`, `local.properties` |
| MCP config with real token | `.cursor/mcp.json` |

## Safe patterns

- Runtime paths: `System.getProperty("user.home")`, `System.getenv("APPDATA")`
- Docs: describe paths generically (`~/Library/Application Support/GameLauncher`)
- CI: use `${{ runner.temp }}` / GitHub-hosted paths only
- Manifest URLs: public CDN endpoints without auth query strings

## Config layering

| Committed | Local only |
|-----------|------------|
| `.env.example` (empty values) | `.env` |
| `.cursor/mcp.json.example` | `.cursor/mcp.json` |
| `Config.kt` placeholders | Keychain → env vars |

## PR / issue hygiene

- Reference issue numbers (`Closes #3`) — fine
- Never paste PATs, private bucket names, or signing keys in PR descriptions

## Scan enforcement

`scripts/scan-secrets.sh` flags concrete home-directory paths in staged files. Fix before commit.
