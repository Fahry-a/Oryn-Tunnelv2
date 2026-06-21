package net.oryn.mc.orynTunnelv2.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Handles /otunnel commands for tunnel management.
 * Uses Adventure Component API for modern text rendering.
 */
public class TunnelCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final TunnelManager tunnelManager;
    private final TunnelGUI tunnelGUI;
    private final String defaultHelpPrefix;

    private static final TextColor GOLD = NamedTextColor.GOLD;
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;
    private static final TextColor AQUA = NamedTextColor.AQUA;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor WHITE = NamedTextColor.WHITE;

    private static Component prefix() {
        return Component.text("[OrynTunnel] ", GOLD);
    }

    private static Component success(String text) {
        return Component.text(text, GREEN);
    }

    private static Component warning(String text) {
        return Component.text(text, YELLOW);
    }

    private static Component error(String text) {
        return Component.text(text, RED);
    }

    private static Component info(String text) {
        return Component.text(text, AQUA);
    }

    private static Component header(String text) {
        return Component.text(text, GOLD);
    }

    private static Component dim(String text) {
        return Component.text(text, GRAY);
    }

    public TunnelCommand(JavaPlugin plugin, TunnelManager tunnelManager, TunnelGUI tunnelGUI) {
        this(plugin, tunnelManager, tunnelGUI, "/otunnel help");
    }

    public TunnelCommand(JavaPlugin plugin, TunnelManager tunnelManager, TunnelGUI tunnelGUI, String defaultHelpPrefix) {
        this.plugin = plugin;
        this.tunnelManager = tunnelManager;
        this.tunnelGUI = tunnelGUI;
        this.defaultHelpPrefix = defaultHelpPrefix;
    }

    private CloudflaredManager cloudflaredManager() {
        return tunnelManager.getCloudflaredManager();
    }

    private ConfigManager configManager() {
        return tunnelManager.getConfigManager();
    }

    private TunnelHealthChecker healthChecker() {
        return tunnelManager.getHealthChecker();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return handleCommand(sender, args);
    }

    public boolean onModuleCommand(CommandSender sender, String label, String[] args) {
        return handleCommand(sender, args);
    }

    private boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            sender.sendMessage(prefix().append(error("You don't have permission to use this command!")));
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
                    sender.sendMessage(prefix().append(error("GUI is only available for players!")));
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
                sender.sendMessage(prefix().append(error("Unknown subcommand. Use " + defaultHelpPrefix)));
                break;
        }

        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(header("========== Oryn Tunnel Status =========="));

        CloudflaredManager cm = cloudflaredManager();
        String localVersion = cm.getLocalVersion();
        sender.sendMessage(info("Cloudflared Version: ").append(Component.text(localVersion != null ? localVersion : "Not installed", WHITE)));

        boolean binaryExists = cm.isBinaryExists();
        sender.sendMessage(info("Binary: ").append(binaryExists ? success("Found") : error("Not found")));

        boolean tunnelRunning = cm.isRunning();
        sender.sendMessage(info("Tunnel: ").append(tunnelRunning ? success("Running") : error("Stopped")));

        if (tunnelRunning) {
            boolean tunnelConnected = cm.isConnected();
            sender.sendMessage(info("Connected: ").append(tunnelConnected ? success("Yes") : warning("Waiting...")));
            sender.sendMessage(info("Uptime: ").append(Component.text(cm.getUptimeFormatted(), WHITE)));
            Process process = cm.getProcess();
            if (process != null) {
                sender.sendMessage(info("PID: ").append(Component.text(process.pid(), WHITE)));
            }
        }

        boolean tokenConfigured = configManager().isTokenConfigured();
        sender.sendMessage(info("Token: ").append(tokenConfigured ? success("Configured") : error("Not configured")));

        if (cm.getLastError() != null) {
            sender.sendMessage(error("Last Error: ").append(dim(cm.getLastError())));
        }

        sender.sendMessage(header("========================================="));
    }

    private void showStats(CommandSender sender) {
        sender.sendMessage(header("========== Oryn Tunnel Stats =========="));

        CloudflaredManager cm = cloudflaredManager();
        String localVersion = cm.getLocalVersion();
        sender.sendMessage(info("Version: ").append(Component.text(localVersion != null ? localVersion : "N/A", WHITE)));

        boolean tunnelRunning = cm.isRunning();
        sender.sendMessage(info("Status: ").append(tunnelRunning ? success("Running") : error("Stopped")));

        if (tunnelRunning) {
            sender.sendMessage(info("Connected: ").append(cm.isConnected() ? success("Yes") : warning("Waiting...")));
            sender.sendMessage(info("Uptime: ").append(Component.text(cm.getUptimeFormatted(), WHITE)));
            Process process = cm.getProcess();
            if (process != null) {
                sender.sendMessage(info("PID: ").append(Component.text(process.pid(), WHITE)));
            }
        }

        sender.sendMessage(info("Total Restarts: ").append(Component.text(cm.getTotalRestarts(), WHITE)));

        if (cm.getRetryCount() > 0) {
            sender.sendMessage(info("Current Retry: ").append(warning(cm.getRetryCount() + "/" + configManager().getMaxRetries())));
        }

        sender.sendMessage(info("Health Check: ").append(Component.text(healthChecker().isEnabled() ? "Active" : "Inactive", WHITE)));
        sender.sendMessage(info("Health Interval: ").append(Component.text(configManager().getHealthCheckInterval() + "s", WHITE)));
        sender.sendMessage(info("Auto-update: ").append(Component.text(configManager().isAutoUpdate() ? "Enabled" : "Disabled", WHITE)));

        String pinnedVersion = configManager().getCloudflaredVersion();
        if (pinnedVersion != null) {
            sender.sendMessage(info("Pinned Version: ").append(Component.text(pinnedVersion, WHITE)));
        }

        sender.sendMessage(header("========================================="));
    }

    private void startTunnel(CommandSender sender) {
        CloudflaredManager cm = cloudflaredManager();
        if (cm.isRunning()) {
            sender.sendMessage(prefix().append(warning("Tunnel is already running!")));
            return;
        }

        String token = configManager().getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(prefix().append(error("Token is not configured! Edit config.yml")));
            return;
        }

        if (!cm.isBinaryExists()) {
            sender.sendMessage(prefix().append(warning("Binary not found, downloading...")));
            cm.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sendSafe(sender, prefix().append(info("Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")")));
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sendSafe(sender, prefix().append(success("Download complete! Starting tunnel...")));
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            cm.resetAutoRestart();
                            cm.startTunnel(token);
                            healthChecker().start();
                            sendSafe(sender, prefix().append(success("Tunnel started!")));
                        });
                    } else {
                        sendSafe(sender, prefix().append(error("Failed to download cloudflared!")));
                    }
                }
            });
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> cm.ensureBinary(configManager().getCloudflaredVersion()));
            return;
        }

        cm.resetAutoRestart();
        cm.startTunnel(token);
        healthChecker().start();
        sender.sendMessage(prefix().append(success("Tunnel started!")));
    }

    private void stopTunnel(CommandSender sender) {
        if (!cloudflaredManager().isRunning()) {
            sender.sendMessage(prefix().append(warning("Tunnel is not running!")));
            return;
        }

        tunnelManager.stopTunnel();
        sender.sendMessage(prefix().append(success("Tunnel stopped!")));
    }

    private void restartTunnel(CommandSender sender) {
        CloudflaredManager cm = cloudflaredManager();
        if (!cm.isRunning()) {
            sender.sendMessage(prefix().append(warning("Tunnel is not running! Use start")));
            return;
        }

        String token = configManager().getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(prefix().append(error("Token is not configured!")));
            return;
        }

        sender.sendMessage(prefix().append(info("Restarting tunnel...")));
        cm.stopTunnel();
        cm.resetAutoRestart();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cm.startTunnel(token);
            healthChecker().start();
            sender.sendMessage(prefix().append(success("Tunnel restarted!")));
        }, 20L);
    }

    private void checkUpdate(CommandSender sender) {
        sender.sendMessage(prefix().append(info("Checking for updates...")));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            CloudflaredManager cm = cloudflaredManager();
            String currentVersion = cm.getLocalVersion();
            String latestVersion = cm.getLatestVersion();

            if (latestVersion == null) {
                sendSafe(sender, prefix().append(error("Could not fetch latest version from GitHub")));
                return;
            }

            if (latestVersion.equals(currentVersion)) {
                sendSafe(sender, prefix().append(success("Cloudflared is up to date (" + currentVersion + ")")));
                return;
            }

            sendSafe(sender, prefix().append(info("New version available: " + latestVersion + " (current: " + currentVersion + ")")));
            sendSafe(sender, prefix().append(info("Downloading...")));

            cm.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sendSafe(sender, prefix().append(info("Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")")));
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sendSafe(sender, prefix().append(success("Update complete! Restart tunnel to use new version.")));
                    } else {
                        sendSafe(sender, prefix().append(error("Update failed!")));
                    }
                }
            });

            cm.downloadBinary(latestVersion);
        });
    }

    private void reloadConfig(CommandSender sender) {
        tunnelManager.reloadConfig();
        sender.sendMessage(prefix().append(success("Configuration reloaded!")));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(header("========== Oryn Tunnel Commands =========="));
        sender.sendMessage(info("/otunnel ").append(dim("- Open GUI")));
        sender.sendMessage(info("/otunnel status ").append(dim("- Check tunnel status")));
        sender.sendMessage(info("/otunnel stats ").append(dim("- Show detailed statistics")));
        sender.sendMessage(info("/otunnel start ").append(dim("- Start tunnel")));
        sender.sendMessage(info("/otunnel stop ").append(dim("- Stop tunnel")));
        sender.sendMessage(info("/otunnel restart ").append(dim("- Restart tunnel")));
        sender.sendMessage(info("/otunnel update ").append(dim("- Check and update cloudflared")));
        sender.sendMessage(info("/otunnel reload ").append(dim("- Reload configuration")));
        sender.sendMessage(info("/otunnel help ").append(dim("- Show this help")));
        sender.sendMessage(header("==========================================="));
    }

    private void sendSafe(CommandSender sender, Component message) {
        if (sender instanceof Player player) {
            if (player.isOnline()) {
                player.sendMessage(message);
            }
        } else {
            sender.sendMessage(message);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return handleTabComplete(sender, args);
    }

    public List<String> onModuleTabComplete(CommandSender sender, String label, String[] args) {
        return handleTabComplete(sender, args);
    }

    private List<String> handleTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subcommands = List.of("gui", "status", "stats", "start", "stop", "restart", "update", "reload", "help");
            String input = args[0].toLowerCase();
            return subcommands.stream()
                .filter(cmd -> cmd.startsWith(input))
                .toList();
        }

        return Collections.emptyList();
    }
}
