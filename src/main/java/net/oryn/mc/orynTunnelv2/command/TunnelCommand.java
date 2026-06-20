package net.oryn.mc.orynTunnelv2.command;

import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class TunnelCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final TunnelManager tunnelManager;
    private final TunnelGUI tunnelGUI;
    private final String defaultHelpPrefix;

    private static final String PREFIX = ChatColor.GOLD + "[OrynTunnel] " + ChatColor.RESET;
    private static final String SUCCESS = ChatColor.GREEN + "";
    private static final String WARNING = ChatColor.YELLOW + "";
    private static final String ERROR = ChatColor.RED + "";
    private static final String INFO = ChatColor.AQUA + "";
    private static final String HEADER = ChatColor.GOLD + "";
    private static final String DIM = ChatColor.GRAY + "";

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
                sender.sendMessage(PREFIX + ERROR + "Unknown subcommand. Use " + defaultHelpPrefix);
                break;
        }

        return true;
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Status ==========");

        CloudflaredManager cm = cloudflaredManager();
        String localVersion = cm.getLocalVersion();
        sender.sendMessage(INFO + "Cloudflared Version: " + ChatColor.WHITE + (localVersion != null ? localVersion : "Not installed"));

        boolean binaryExists = cm.isBinaryExists();
        sender.sendMessage(INFO + "Binary: " + (binaryExists ? SUCCESS + "Found" : ERROR + "Not found"));

        boolean tunnelRunning = cm.isRunning();
        sender.sendMessage(INFO + "Tunnel: " + (tunnelRunning ? SUCCESS + "Running" : ERROR + "Stopped"));

        if (tunnelRunning) {
            boolean tunnelConnected = cm.isConnected();
            sender.sendMessage(INFO + "Connected: " + (tunnelConnected ? SUCCESS + "Yes" : WARNING + "Waiting..."));
            sender.sendMessage(INFO + "Uptime: " + ChatColor.WHITE + cm.getUptimeFormatted());
        }

        boolean tokenConfigured = configManager().isTokenConfigured();
        sender.sendMessage(INFO + "Token: " + (tokenConfigured ? SUCCESS + "Configured" : ERROR + "Not configured"));

        if (cm.getLastError() != null) {
            sender.sendMessage(ERROR + "Last Error: " + DIM + cm.getLastError());
        }

        sender.sendMessage(HEADER + "=========================================");
    }

    private void showStats(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Stats ==========");

        CloudflaredManager cm = cloudflaredManager();
        String localVersion = cm.getLocalVersion();
        sender.sendMessage(INFO + "Version: " + ChatColor.WHITE + (localVersion != null ? localVersion : "N/A"));

        boolean tunnelRunning = cm.isRunning();
        sender.sendMessage(INFO + "Status: " + (tunnelRunning ? SUCCESS + "Running" : ERROR + "Stopped"));

        if (tunnelRunning) {
            sender.sendMessage(INFO + "Connected: " + (cm.isConnected() ? SUCCESS + "Yes" : WARNING + "Waiting..."));
            sender.sendMessage(INFO + "Uptime: " + ChatColor.WHITE + cm.getUptimeFormatted());
            sender.sendMessage(INFO + "PID: " + ChatColor.WHITE + cm.getProcess().pid());
        }

        sender.sendMessage(INFO + "Total Restarts: " + ChatColor.WHITE + cm.getTotalRestarts());

        if (cm.getRetryCount() > 0) {
            sender.sendMessage(INFO + "Current Retry: " + WARNING + cm.getRetryCount() + "/" + configManager().getMaxRetries());
        }

        sender.sendMessage(INFO + "Health Check: " + ChatColor.WHITE + (healthChecker().isEnabled() ? SUCCESS + "Active" : DIM + "Inactive"));
        sender.sendMessage(INFO + "Health Interval: " + ChatColor.WHITE + configManager().getHealthCheckInterval() + "s");
        sender.sendMessage(INFO + "Auto-update: " + ChatColor.WHITE + (configManager().isAutoUpdate() ? "Enabled" : "Disabled"));

        String pinnedVersion = configManager().getCloudflaredVersion();
        if (pinnedVersion != null) {
            sender.sendMessage(INFO + "Pinned Version: " + ChatColor.WHITE + pinnedVersion);
        }

        sender.sendMessage(HEADER + "=========================================");
    }

    private void startTunnel(CommandSender sender) {
        CloudflaredManager cm = cloudflaredManager();
        if (cm.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is already running!");
            return;
        }

        String token = configManager().getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(PREFIX + ERROR + "Token is not configured! Edit config.yml");
            return;
        }

        if (!cm.isBinaryExists()) {
            sender.sendMessage(PREFIX + WARNING + "Binary not found, downloading...");
            cm.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sendSafe(sender, PREFIX + INFO + "Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")");
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sendSafe(sender, PREFIX + SUCCESS + "Download complete! Starting tunnel...");
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            cm.resetAutoRestart();
                            cm.startTunnel(token);
                            healthChecker().start();
                            sendSafe(sender, PREFIX + SUCCESS + "Tunnel started!");
                        });
                    } else {
                        sendSafe(sender, PREFIX + ERROR + "Failed to download cloudflared!");
                    }
                }
            });
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> cm.ensureBinary(configManager().getCloudflaredVersion()));
            return;
        }

        cm.resetAutoRestart();
        cm.startTunnel(token);
        healthChecker().start();
        sender.sendMessage(PREFIX + SUCCESS + "Tunnel started!");
    }

    private void stopTunnel(CommandSender sender) {
        if (!cloudflaredManager().isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is not running!");
            return;
        }

        tunnelManager.stopTunnel();
        sender.sendMessage(PREFIX + SUCCESS + "Tunnel stopped!");
    }

    private void restartTunnel(CommandSender sender) {
        CloudflaredManager cm = cloudflaredManager();
        if (!cm.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is not running! Use start");
            return;
        }

        String token = configManager().getToken();
        if (token == null || token.isBlank()) {
            sender.sendMessage(PREFIX + ERROR + "Token is not configured!");
            return;
        }

        sender.sendMessage(PREFIX + INFO + "Restarting tunnel...");
        cm.stopTunnel();
        cm.resetAutoRestart();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            cm.startTunnel(token);
            healthChecker().start();
            sender.sendMessage(PREFIX + SUCCESS + "Tunnel restarted!");
        }, 20L);
    }

    private void checkUpdate(CommandSender sender) {
        sender.sendMessage(PREFIX + INFO + "Checking for updates...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            CloudflaredManager cm = cloudflaredManager();
            String currentVersion = cm.getLocalVersion();
            String latestVersion = cm.getLatestVersion();

            if (latestVersion == null) {
                sendSafe(sender, PREFIX + ERROR + "Could not fetch latest version from GitHub");
                return;
            }

            if (latestVersion.equals(currentVersion)) {
                sendSafe(sender, PREFIX + SUCCESS + "Cloudflared is up to date (" + currentVersion + ")");
                return;
            }

            sendSafe(sender, PREFIX + INFO + "New version available: " + latestVersion + " (current: " + currentVersion + ")");
            sendSafe(sender, PREFIX + INFO + "Downloading...");

            cm.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sendSafe(sender, PREFIX + INFO + "Download: " + percent + "% (" + formatBytes(bytesDownloaded) + "/" + formatBytes(totalBytes) + ")");
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sendSafe(sender, PREFIX + SUCCESS + "Update complete! Restart tunnel to use new version.");
                    } else {
                        sendSafe(sender, PREFIX + ERROR + "Update failed!");
                    }
                }
            });

            cm.downloadBinary(latestVersion);
        });
    }

    private void reloadConfig(CommandSender sender) {
        tunnelManager.reloadConfig();
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

    private void sendSafe(CommandSender sender, String message) {
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
