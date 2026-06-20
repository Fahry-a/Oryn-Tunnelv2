# Oryn Tunnel v2

Cloudflare Tunnel plugin for Minecraft servers. Automatically manages cloudflared binary and tunnel connections.

## Features

- **Dual mode** — Runs as standalone plugin (`/otunnel`) or OrynPlugins module (`/oryn module tunnel`)
- **Auto-download** — Downloads cloudflared binary automatically (Linux amd64/arm64)
- **Auto-update** — Checks GitHub for updates and downloads new versions
- **Version pinning** — Pin specific cloudflared version in config
- **SHA256 verification** — Verifies downloaded binary integrity
- **Health check** — Monitors tunnel process + connectivity status
- **Auto-restart** — Restarts tunnel if it crashes (max 5 retries)
- **Tunnel connectivity** — Verifies cloudflared is actually connected to Cloudflare edge
- **Config hot-reload** — Auto-restarts tunnel when token changes via reload
- **GUI** — Full GUI with hover stats and confirmation dialogs
- **Log rotation** — Size-based log rotation with zstd compression
- **Log archiving** — Compresses logs to .zst format on server shutdown

## Requirements

- Paper 1.21.1+ (or compatible fork)
- Java 21+
- Linux (amd64 or arm64)

## Installation

### Standalone (Recommended)

1. Download the latest JAR from [Releases](https://github.com/Fahry-a/Oryn-Tunnelv2/releases)
2. Place in `plugins/` folder
3. Start server to generate config
4. Edit `plugins/Oryn-Tunnelv2/config.yml` with your tunnel token
5. Restart server

### Module (requires OrynPlugins)

1. Install OrynPlugins in `plugins/`
2. Download the latest JAR
3. Place in `plugins/OrynPlugins/modules/`
4. Start server to generate config
5. Edit `plugins/OrynPlugins/modules/tunnel/config.yml` with your tunnel token
6. Restart server

## Configuration

```yaml
# Cloudflare Tunnel Token
token: ""

# Auto-update cloudflared binary
auto-update: true

# Pin specific cloudflared version (leave empty for latest)
cloudflared-version: ""

# Health check interval (seconds)
health-check-interval: 10

# Max auto-restart retries
max-retries: 5

# Max log file size before rotation (bytes, default: 10MB)
log-max-size: 10485760
```

### Config Location

| Mode | Config Path |
|------|------------|
| Standalone | `plugins/Oryn-Tunnelv2/config.yml` |
| Module | `plugins/OrynPlugins/modules/tunnel/config.yml` |

## Commands

### Standalone

| Command | Description |
|---------|-------------|
| `/otunnel` | Open GUI |
| `/otunnel status` | Check tunnel status + connectivity |
| `/otunnel stats` | Show detailed statistics |
| `/otunnel start` | Start tunnel |
| `/otunnel stop` | Stop tunnel |
| `/otunnel restart` | Restart tunnel |
| `/otunnel update` | Check and update cloudflared |
| `/otunnel reload` | Reload configuration |
| `/otunnel help` | Show help |

### Module (via OrynPlugins)

| Command | Description |
|---------|-------------|
| `/oryn module tunnel` | Open GUI |
| `/oryn module tunnel status` | Check tunnel status |
| `/oryn module tunnel start` | Start tunnel |
| `/oryn module tunnel stop` | Stop tunnel |
| `/oryn module tunnel restart` | Restart tunnel |
| `/oryn module tunnel update` | Check and update cloudflared |
| `/oryn module tunnel reload` | Reload configuration |
| `/oryn module list` | List all loaded modules |

## Permissions

| Permission | Description |
|------------|-------------|
| `otunnel.admin` | Access to tunnel commands (default: op) |
| `oryn.admin` | Access to OrynPlugins commands (default: op) |

## GUI

Type `/otunnel` (standalone) or `/oryn module tunnel` (module) in-game to open the GUI. Hover over items to see detailed stats.

- **Status** — Version, uptime, connectivity, errors
- **Statistics** — PID, restarts, health check status
- **Start/Stop/Restart** — With confirmation dialogs
- **Update** — Check and download updates
- **Reload** — Reload configuration (auto-restarts if token changed)

## Architecture

### Standalone Mode

```
Paper Server
  └── Oryn-Tunnelv2 (JavaPlugin)
        └── TunnelManager
              ├── ConfigManager
              ├── LogManager
              ├── CloudflaredManager
              ├── TunnelHealthChecker
              ├── TunnelGUI
              └── TunnelCommand
```

### Module Mode

```
Paper Server
  └── OrynPlugins (JavaPlugin)
        └── ModuleLoader
              └── TunnelModule (OrynModule)
                    └── TunnelManager
                          ├── ConfigManager
                          ├── LogManager
                          ├── CloudflaredManager
                          ├── TunnelHealthChecker
                          ├── TunnelGUI
                          └── TunnelCommand
```

## Building

```bash
./gradlew build
```

Output: `build/libs/Oryn-Tunnelv2-1.2.0.jar`

## Dependencies

- [Paper API](https://papermc.io/) 1.21.1+
- [OrynPlugins](https://github.com/Fahry-a/OrynPlugins) 1.2.0 (for module mode)
- [zstd-jni](https://github.com/luben/zstd-jni) 1.5.7-11

## Changelog

### v1.2.0
- Updated TunnelModule to use `@ModuleInfo` annotation (MODULE-DEVELOPMENT.md compliance)
- Added `Module-Name` attribute to MANIFEST.MF for faster module detection
- Added `onReload()` method for proper module reload handling
- Improved ConfigManager with validator pattern and reload callback support
- Fixed duplicate import in TunnelModule.java
- Added error handling for GUI reload operations
- Improved event listener registration with auto-unregister on disable
- Updated to follow OrynPlugins Module Development Guide best practices

### v1.1
- Fixed thread safety: `AtomicInteger` for counters, `volatile` for shared fields
- Fixed `stopTunnel()` synchronized to prevent concurrent calls
- Fixed player online check before sending async messages
- Deduplicated `onCommand`/`onModuleCommand` code
- Added `TunnelManager` shared class for standalone + module modes
- Added tunnel connectivity check (`isConnected()`)
- Added config hot-reload with auto-restart on token change
- Added binary version pinning (`cloudflared-version` config)
- Added size-based log rotation (`log-max-size` config)
- Added `downloadCallback` thread safety (capture reference before use)
- Fixed `restartTunnel()` counter increment after success verification
- Fixed `plugin.yml` description placeholder
- Removed `resetNotification()` dead code dependency

### v1.0
- Initial release

## License

MIT License
