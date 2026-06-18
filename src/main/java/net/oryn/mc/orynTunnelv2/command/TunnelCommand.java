package net.oryn.mc.orynTunnelv2.command;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class TunnelCommand implements CommandExecutor, TabCompleter {

    private final OrynTunnelv2 plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;
    private final TunnelHealthChecker healthChecker;
    private final TunnelGUI tunnelGUI;

    private static final String PREFIX = ChatColor.GOLD + "[OrynTunnel] " + ChatColor.RESET;
    private static final String SUCCESS = ChatColor.GREEN + "";
    private static final String WARNING = ChatColor.YELLOW + "";
    private static final String ERROR = ChatColor.RED + "";
    private static final String INFO = ChatColor.AQUA + "";
    private static final String HEADER = ChatColor.GOLD + "";
    private static final String DIM = ChatColor.GRAY + "";

    public TunnelCommand(OrynTunnelv2 plugin, CloudflaredManager cloudflaredManager,
                         ConfigManager configManager, TunnelHealthChecker healthChecker,
                         TunnelGUI tunnelGUI) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
        this.configManager = configManager;
        this.healthChecker = healthChecker;
        this.tunnelGUI = tunnelGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            sender.sendMessage(PREFIX + ERROR + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                tunnelGUI.openMainMenu(player);
            } else {
                showHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "gui":
                if (sender instanceof Player player) {
                    tunnelGUI.openMainMenu(player);
                } else {
                    sender.sendMessage(PREFIX + ERROR + "GUI is only available for players!");
                }
                break;
            case "status":
                showStatus(sender);
                break;
            case "stats":
                showStats(sender);
                break;
            case "start":
                startTunnel(sender);
                break;
            case "stop":
                stopTunnel(sender);
                break;
            case "restart":
                restartTunnel(sender);
                break;
            case "update":
                checkUpdate(sender);
                break;
            case "reload":
                reloadConfig(sender);
                break;
            case "help":
                showHelp(sender);
                break;
            default:
                sender.sendMessage(PREFIX + ERROR + "Unknown subcommand. Use /otunnel help");
                break;
        }

        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Status ==========");

        String localVersion = cloudflaredManager.getLocalVersion();
        sender.sendMessage(INFO + "Cloudflared Version: " + ChatColor.WHITE + (localVersion != null ? localVersion : "Not installed"));

        boolean binaryExists = cloudflaredManager.isBinaryExists();
        sender.sendMessage(INFO + "Binary: " + (binaryExists ? SUCCESS + "Found" : ERROR + "Not found"));

        boolean tunnelRunning = cloudflaredManager.isRunning();
        sender.sendMessage(INFO + "Tunnel: " + (tunnelRunning ? SUCCESS + "Running" : ERROR + "Stopped"));

        boolean tokenConfigured = configManager.isTokenConfigured();
        sender.sendMessage(INFO + "Token: " + (tokenConfigured ? SUCCESS + "Configured" : ERROR + "Not configured"));

        if (tunnelRunning) {
            sender.sendMessage(INFO + "Uptime: " + ChatColor.WHITE + cloudflaredManager.getUptimeFormatted());
        }

        if (cloudflaredManager.getLastError() != null) {
            sender.sendMessage(ERROR + "Last Error: " + DIM + cloudflaredManager.getLastError());
        }

        sender.sendMessage(HEADER + "=========================================");
    }

    private void showStats(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Stats ==========");

        String localVersion = cloudflaredManager.getLocalVersion();
        sender.sendMessage(INFO + "Version: " + ChatColor.WHITE + (localVersion != null ? localVersion : "N/A"));

        boolean tunnelRunning = cloudflaredManager.isRunning();
        sender.sendMessage(INFO + "Status: " + (tunnelRunning ? SUCCESS + "Running" : ERROR + "Stopped"));

        if (tunnelRunning) {
            sender.sendMessage(INFO + "Uptime: " + ChatColor.WHITE + cloudflaredManager.getUptimeFormatted());
            sender.sendMessage(INFO + "PID: " + ChatColor.WHITE + cloudflaredManager.getProcess().pid());
        }

        sender.sendMessage(INFO + "Total Restarts: " + ChatColor.WHITE + cloudflaredManager.getTotalRestarts());

        if (cloudflaredManager.getRetryCount() > 0) {
            sender.sendMessage(INFO + "Current Retry: " + WARNING + cloudflaredManager.getRetryCount() + "/" + configManager.getMaxRetries());
        }

        sender.sendMessage(INFO + "Health Check: " + ChatColor.WHITE + (healthChecker.isEnabled() ? SUCCESS + "Active" : DIM + "Inactive"));
        sender.sendMessage(INFO + "Health Interval: " + ChatColor.WHITE + configManager.getHealthCheckInterval() + "s");
        sender.sendMessage(INFO + "Auto-update: " + ChatColor.WHITE + (configManager.isAutoUpdate() ? "Enabled" : "Disabled"));

        sender.sendMessage(HEADER + "=========================================");
    }

    private void startTunnel(CommandSender sender) {
        if (cloudflaredManager.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is already running!");
            return;
        }

        String token = configManager.getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(PREFIX + ERROR + "Token is not configured! Edit config.yml");
            return;
        }

        if (!cloudflaredManager.isBinaryExists()) {
            sender.sendMessage(PREFIX + WARNING + "Binary not found, downloading...");
            cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sender.sendMessage(PREFIX + INFO + "Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")");
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sender.sendMessage(PREFIX + SUCCESS + "Download complete! Starting tunnel...");
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            cloudflaredManager.resetAutoRestart();
                            cloudflaredManager.startTunnel(token);
                            healthChecker.start();
                            sender.sendMessage(PREFIX + SUCCESS + "Tunnel started!");
                        });
                    } else {
                        sender.sendMessage(PREFIX + ERROR + "Failed to download cloudflared!");
                    }
                }
            });
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, cloudflaredManager::ensureBinary);
            return;
        }

        cloudflaredManager.resetAutoRestart();
        cloudflaredManager.startTunnel(token);
        healthChecker.start();
        sender.sendMessage(PREFIX + SUCCESS + "Tunnel started!");
    }

    private void stopTunnel(CommandSender sender) {
        if (!cloudflaredManager.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is not running!");
            return;
        }

        healthChecker.stop();
        cloudflaredManager.stopTunnel();
        sender.sendMessage(PREFIX + SUCCESS + "Tunnel stopped!");
    }

    private void restartTunnel(CommandSender sender) {
        if (!cloudflaredManager.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is not running! Use /otunnel start");
            return;
        }

        String token = configManager.getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(PREFIX + ERROR + "Token is not configured!");
            return;
        }

        sender.sendMessage(PREFIX + INFO + "Restarting tunnel...");
        cloudflaredManager.stopTunnel();
        cloudflaredManager.resetAutoRestart();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
            sender.sendMessage(PREFIX + SUCCESS + "Tunnel restarted!");
        }, 20L);
    }

    private void checkUpdate(CommandSender sender) {
        sender.sendMessage(PREFIX + INFO + "Checking for updates...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String currentVersion = cloudflaredManager.getLocalVersion();
            String latestVersion = cloudflaredManager.getLatestVersion();

            if (latestVersion == null) {
                sender.sendMessage(PREFIX + ERROR + "Could not fetch latest version from GitHub");
                return;
            }

            if (latestVersion.equals(currentVersion)) {
                sender.sendMessage(PREFIX + SUCCESS + "Cloudflared is up to date (" + currentVersion + ")");
                return;
            }

            sender.sendMessage(PREFIX + INFO + "New version available: " + latestVersion + " (current: " + currentVersion + ")");
            sender.sendMessage(PREFIX + INFO + "Downloading...");

            cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sender.sendMessage(PREFIX + INFO + "Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")");
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sender.sendMessage(PREFIX + SUCCESS + "Update complete! Restart tunnel to use new version.");
                    } else {
                        sender.sendMessage(PREFIX + ERROR + "Update failed!");
                    }
                }
            });

            cloudflaredManager.downloadBinary(latestVersion);
        });
    }

    private void reloadConfig(CommandSender sender) {
        configManager.reload();
        sender.sendMessage(PREFIX + SUCCESS + "Configuration reloaded!");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Commands ==========");
        sender.sendMessage(INFO + "/otunnel " + DIM + "- Open GUI");
        sender.sendMessage(INFO + "/otunnel status " + DIM + "- Check tunnel status");
        sender.sendMessage(INFO + "/otunnel stats " + DIM + "- Show detailed statistics");
        sender.sendMessage(INFO + "/otunnel start " + DIM + "- Start tunnel");
        sender.sendMessage(INFO + "/otunnel stop " + DIM + "- Stop tunnel");
        sender.sendMessage(INFO + "/otunnel restart " + DIM + "- Restart tunnel");
        sender.sendMessage(INFO + "/otunnel update " + DIM + "- Check and update cloudflared");
        sender.sendMessage(INFO + "/otunnel reload " + DIM + "- Reload configuration");
        sender.sendMessage(INFO + "/otunnel help " + DIM + "- Show this help");
        sender.sendMessage(HEADER + "===========================================");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return List.of("gui", "status", "stats", "start", "stop", "restart", "update", "reload", "help");
        }

        return Collections.emptyList();
    }
}
