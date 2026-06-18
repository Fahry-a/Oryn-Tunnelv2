package net.oryn.mc.orynTunnelv2.command;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.log.LogManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class TunnelCommand implements CommandExecutor, TabCompleter {

    private final OrynTunnelv2 plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final TunnelHealthChecker healthChecker;

    private static final String PREFIX = ChatColor.GOLD + "[OrynTunnel] " + ChatColor.RESET;
    private static final String SUCCESS = ChatColor.GREEN + "";
    private static final String WARNING = ChatColor.YELLOW + "";
    private static final String ERROR = ChatColor.RED + "";
    private static final String INFO = ChatColor.AQUA + "";
    private static final String HEADER = ChatColor.GOLD + "";

    public TunnelCommand(OrynTunnelv2 plugin, CloudflaredManager cloudflaredManager,
                         ConfigManager configManager, LogManager logManager,
                         TunnelHealthChecker healthChecker) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
        this.configManager = configManager;
        this.logManager = logManager;
        this.healthChecker = healthChecker;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            sender.sendMessage(PREFIX + ERROR + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "status":
                showStatus(sender);
                break;
            case "start":
                startTunnel(sender);
                break;
            case "stop":
                stopTunnel(sender);
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

        sender.sendMessage(INFO + "Auto-update: " + ChatColor.WHITE + configManager.isAutoUpdate());
        sender.sendMessage(INFO + "Health Check: " + ChatColor.WHITE + configManager.getHealthCheckInterval() + "s");
        sender.sendMessage(INFO + "Max Retries: " + ChatColor.WHITE + configManager.getMaxRetries());

        if (cloudflaredManager.getRetryCount() > 0) {
            sender.sendMessage(WARNING + "Retry Count: " + cloudflaredManager.getRetryCount());
        }

        sender.sendMessage(HEADER + "=========================================");
    }

    private void startTunnel(CommandSender sender) {
        if (cloudflaredManager.isRunning()) {
            sender.sendMessage(PREFIX + WARNING + "Tunnel is already running!");
            return;
        }

        String token = configManager.getToken();
        if (token == null || token.isEmpty() || token.isBlank()) {
            sender.sendMessage(PREFIX + ERROR + "Token is not configured! Edit config.yml");
            return;
        }

        if (!cloudflaredManager.isBinaryExists()) {
            sender.sendMessage(PREFIX + WARNING + "Binary not found, downloading...");
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                if (cloudflaredManager.ensureBinary()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        cloudflaredManager.resetAutoRestart();
                        cloudflaredManager.startTunnel(token);
                        healthChecker.start();
                        sender.sendMessage(PREFIX + SUCCESS + "Tunnel started!");
                    });
                } else {
                    sender.sendMessage(PREFIX + ERROR + "Failed to download cloudflared!");
                }
            });
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

            if (cloudflaredManager.downloadBinary(latestVersion)) {
                sender.sendMessage(PREFIX + SUCCESS + "Update complete! Restart tunnel to use new version.");
            } else {
                sender.sendMessage(PREFIX + ERROR + "Update failed!");
            }
        });
    }

    private void reloadConfig(CommandSender sender) {
        configManager.reload();
        sender.sendMessage(PREFIX + SUCCESS + "Configuration reloaded!");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(HEADER + "========== Oryn Tunnel Commands ==========");
        sender.sendMessage(INFO + "/otunnel status " + ChatColor.GRAY + "- Check tunnel status");
        sender.sendMessage(INFO + "/otunnel start " + ChatColor.GRAY + "- Start tunnel");
        sender.sendMessage(INFO + "/otunnel stop " + ChatColor.GRAY + "- Stop tunnel");
        sender.sendMessage(INFO + "/otunnel update " + ChatColor.GRAY + "- Check and update cloudflared");
        sender.sendMessage(INFO + "/otunnel reload " + ChatColor.GRAY + "- Reload configuration");
        sender.sendMessage(INFO + "/otunnel help " + ChatColor.GRAY + "- Show this help");
        sender.sendMessage(HEADER + "===========================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("otunnel.admin")) {
            return Arrays.asList();
        }

        if (args.length == 1) {
            return Arrays.asList("status", "start", "stop", "update", "reload", "help");
        }

        return Arrays.asList();
    }
}
