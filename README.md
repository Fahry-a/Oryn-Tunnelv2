# Oryn Tunnel v2

Cloudflare Tunnel plugin for Minecraft servers. Automatically manages cloudflared binary and tunnel connections.

## Features

- **Auto-download** - Downloads cloudflared binary automatically (Linux amd64/arm64)
- **Auto-update** - Checks GitHub for updates and downloads new versions
- **SHA256 verification** - Verifies downloaded binary integrity
- **Health check** - Monitors tunnel process every 10 seconds
- **Auto-restart** - Restarts tunnel if it crashes (max 5 retries)
- **GUI** - Full GUI with hover stats and confirmation dialogs
- **Log archiving** - Compresses logs to .zst format on server shutdown

## Requirements

- Paper 1.21.1+ (or compatible fork)
- Java 21+
- Linux (amd64 or arm64)

## Installation

1. Download the latest JAR from [Releases](https://github.com/Fahry-a/Oryn-Tunnelv2/releases)
2. Place in `plugins/` folder
3. Start server to generate config
4. Edit `plugins/Oryn-Tunnelv2/config.yml` with your tunnel token
5. Restart server

## Configuration

```yaml
# Cloudflare Tunnel Token
# Get from: https://dash.cloudflare.com/zerotrust/networks/tunnels
token: ""

# Auto-update cloudflared binary
auto-update: true

# Health check interval (seconds)
health-check-interval: 10

# Max auto-restart retries
max-retries: 5
```

## Commands

| Command | Description |
|---------|-------------|
| `/otunnel` | Open GUI |
| `/otunnel status` | Check tunnel status |
| `/otunnel stats` | Show detailed statistics |
| `/otunnel start` | Start tunnel |
| `/otunnel stop` | Stop tunnel |
| `/otunnel restart` | Restart tunnel |
| `/otunnel update` | Check and update cloudflared |
| `/otunnel reload` | Reload configuration |
| `/otunnel help` | Show help |

## Permissions

| Permission | Description |
|------------|-------------|
| `otunnel.admin` | Access to all tunnel commands (default: op) |

## GUI

Type `/otunnel` in-game to open the GUI. Hover over items to see detailed stats.

- **Status** - Version, uptime, errors
- **Statistics** - PID, restarts, health check status
- **Start/Stop/Restart** - With confirmation dialogs
- **Update** - Check and download updates
- **Reload** - Reload configuration

## Building

```bash
./gradlew build
```

Output: `build/libs/Oryn-Tunnelv2-1.0.jar`

## License

MIT License
