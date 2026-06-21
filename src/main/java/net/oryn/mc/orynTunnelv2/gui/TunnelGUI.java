package net.oryn.mc.orynTunnelv2.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates and manages the tunnel management GUI.
 * Uses Adventure Component API for modern text rendering.
 */
public class TunnelGUI {

    private final JavaPlugin plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;

    private static final TextColor GOLD = NamedTextColor.GOLD;
    private static final TextColor GREEN = NamedTextColor.GREEN;
    private static final TextColor RED = NamedTextColor.RED;
    private static final TextColor YELLOW = NamedTextColor.YELLOW;
    private static final TextColor AQUA = NamedTextColor.AQUA;
    private static final TextColor GRAY = NamedTextColor.GRAY;
    private static final TextColor WHITE = NamedTextColor.WHITE;

    private static final String TITLE_PREFIX = "\u00A7" + "6\u00A7" + "l";

    public static final String MAIN_MENU_TITLE = TITLE_PREFIX + "Oryn Tunnel v2";
    public static final String STATUS_TITLE = TITLE_PREFIX + "Tunnel Status";
    public static final String STATS_TITLE = TITLE_PREFIX + "Tunnel Statistics";
    public static final String START_CONFIRM_TITLE = TITLE_PREFIX + "Confirm Start";
    public static final String STOP_CONFIRM_TITLE = TITLE_PREFIX + "Confirm Stop";
    public static final String RESTART_CONFIRM_TITLE = TITLE_PREFIX + "Confirm Restart";
    public static final String UPDATE_TITLE = TITLE_PREFIX + "Update Cloudflared";
    public static final String RELOAD_CONFIRM_TITLE = TITLE_PREFIX + "Confirm Reload";
    public static final String HELP_TITLE = TITLE_PREFIX + "Help & Commands";

    public TunnelGUI(JavaPlugin plugin, CloudflaredManager cloudflaredManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
        this.configManager = configManager;
    }

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, MAIN_MENU_TITLE);

        for (int i = 0; i < 54; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        gui.setItem(10, createItem(Material.ENDER_PEARL, GOLD + "Tunnel Status",
            GRAY + "Click to view detailed status",
            WHITE + "Shows: version, uptime, errors"));

        gui.setItem(11, createItem(Material.BARREL, GOLD + "Tunnel Statistics",
            GRAY + "Click to view statistics",
            WHITE + "Shows: uptime, restarts, PID"));

        gui.setItem(13, createItem(Material.LIME_DYE, GREEN + "Start Tunnel",
            GRAY + "Click to start the tunnel",
            WHITE + "Requires: configured token"));

        gui.setItem(14, createItem(Material.RED_DYE, RED + "Stop Tunnel",
            GRAY + "Click to stop the tunnel",
            WHITE + "Graceful shutdown"));

        gui.setItem(15, createItem(Material.YELLOW_DYE, YELLOW + "Restart Tunnel",
            GRAY + "Click to restart the tunnel",
            WHITE + "Stop then start"));

        gui.setItem(28, createItem(Material.PAPER, AQUA + "Update Cloudflared",
            GRAY + "Check and download updates",
            WHITE + "Verifies SHA256 checksum"));

        gui.setItem(29, createItem(Material.COMPARATOR, GOLD + "Reload Config",
            GRAY + "Click to reload configuration",
            WHITE + "Reloads config.yml"));

        gui.setItem(31, createItem(Material.BOOK, GOLD + "Help & Commands",
            GRAY + "Click to view all commands",
            WHITE + "Command reference"));

        gui.setItem(49, createItem(Material.BARRIER, RED + "Close",
            GRAY + "Click to close"));

        player.openInventory(gui);
    }

    public void openStatusGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, STATUS_TITLE);

        fillBackground(gui);

        String localVersion = cloudflaredManager.getLocalVersion();
        boolean binaryExists = cloudflaredManager.isBinaryExists();
        boolean tunnelRunning = cloudflaredManager.isRunning();
        boolean tokenConfigured = configManager.isTokenConfigured();

        gui.setItem(13, createItem(Material.NETHER_STAR, GOLD + "\u00A7lOryn Tunnel Status",
            "",
            AQUA + "Version: " + WHITE + (localVersion != null ? localVersion : "Not installed"),
            AQUA + "Binary: " + (binaryExists ? GREEN + "Found" : RED + "Not found"),
            AQUA + "Tunnel: " + (tunnelRunning ? GREEN + "Running" : RED + "Stopped"),
            AQUA + "Token: " + (tokenConfigured ? GREEN + "Configured" : RED + "Not configured")
        ));

        gui.setItem(20, createItem(Material.CLOCK, AQUA + "Uptime",
            "",
            WHITE + "Current: " + GREEN + cloudflaredManager.getUptimeFormatted()
        ));

        gui.setItem(22, createItem(Material.REDSTONE, AQUA + "Health Check",
            "",
            WHITE + "Status: " + (tunnelRunning ? GREEN + "Active" : RED + "Inactive"),
            WHITE + "Interval: " + configManager.getHealthCheckInterval() + "s"
        ));

        gui.setItem(24, createItem(Material.REPEATER, AQUA + "Auto-Update",
            "",
            WHITE + "Enabled: " + (configManager.isAutoUpdate() ? GREEN + "Yes" : RED + "No")
        ));

        String lastError = cloudflaredManager.getLastError();
        if (lastError != null) {
            gui.setItem(31, createItem(Material.BARRIER, RED + "Last Error",
                "",
                RED + lastError
            ));
        }

        gui.setItem(49, createItem(Material.ARROW, GOLD + "Back to Main Menu"));

        player.openInventory(gui);
    }

    public void openStatsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, STATS_TITLE);

        fillBackground(gui);

        String localVersion = cloudflaredManager.getLocalVersion();
        boolean tunnelRunning = cloudflaredManager.isRunning();

        gui.setItem(13, createItem(Material.NETHER_STAR, GOLD + "\u00A7lTunnel Statistics",
            "",
            AQUA + "Version: " + WHITE + (localVersion != null ? localVersion : "N/A"),
            AQUA + "Status: " + (tunnelRunning ? GREEN + "Running" : RED + "Stopped"),
            tunnelRunning && cloudflaredManager.getProcess() != null ? AQUA + "PID: " + WHITE + cloudflaredManager.getProcess().pid() : "",
            tunnelRunning ? AQUA + "Uptime: " + WHITE + cloudflaredManager.getUptimeFormatted() : "",
            "",
            AQUA + "Total Restarts: " + WHITE + cloudflaredManager.getTotalRestarts(),
            cloudflaredManager.getRetryCount() > 0 ? AQUA + "Current Retry: " + YELLOW + cloudflaredManager.getRetryCount() + "/" + configManager.getMaxRetries() : "",
            "",
            AQUA + "Health Check: " + WHITE + configManager.getHealthCheckInterval() + "s",
            AQUA + "Max Retries: " + WHITE + configManager.getMaxRetries(),
            AQUA + "Auto-Update: " + WHITE + (configManager.isAutoUpdate() ? "Enabled" : "Disabled")
        ));

        gui.setItem(49, createItem(Material.ARROW, GOLD + "Back to Main Menu"));

        player.openInventory(gui);
    }

    public void openStartConfirmGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, START_CONFIRM_TITLE);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        gui.setItem(11, createItem(Material.LIME_DYE, GREEN + "Confirm Start",
            "",
            WHITE + "Start the cloudflared tunnel?",
            "",
            cloudflaredManager.isRunning() ? RED + "Tunnel is already running!" : GREEN + "Ready to start"
        ));

        gui.setItem(15, createItem(Material.RED_DYE, RED + "Cancel",
            "",
            GRAY + "Click to cancel"
        ));

        player.openInventory(gui);
    }

    public void openStopConfirmGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, STOP_CONFIRM_TITLE);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        gui.setItem(11, createItem(Material.RED_DYE, RED + "Confirm Stop",
            "",
            WHITE + "Stop the cloudflared tunnel?",
            "",
            cloudflaredManager.isRunning() ? GREEN + "Tunnel is running" : RED + "Tunnel is not running"
        ));

        gui.setItem(15, createItem(Material.LIME_DYE, GREEN + "Cancel",
            "",
            GRAY + "Click to cancel"
        ));

        player.openInventory(gui);
    }

    public void openRestartConfirmGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, RESTART_CONFIRM_TITLE);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        gui.setItem(11, createItem(Material.YELLOW_DYE, YELLOW + "Confirm Restart",
            "",
            WHITE + "Restart the cloudflared tunnel?",
            "",
            cloudflaredManager.isRunning() ? GREEN + "Tunnel is running" : RED + "Tunnel is not running"
        ));

        gui.setItem(15, createItem(Material.RED_DYE, RED + "Cancel",
            "",
            GRAY + "Click to cancel"
        ));

        player.openInventory(gui);
    }

    public void openUpdateGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, UPDATE_TITLE);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        String currentVersion = cloudflaredManager.getLocalVersion();
        gui.setItem(11, createItem(Material.PAPER, AQUA + "Current Version",
            "",
            WHITE + "Version: " + (currentVersion != null ? GREEN + currentVersion : RED + "Not installed")
        ));

        gui.setItem(15, createItem(Material.EMERALD, GREEN + "Check for Updates",
            "",
            GRAY + "Click to check GitHub",
            GRAY + "for latest version"
        ));

        player.openInventory(gui);
    }

    public void openReloadConfirmGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, RELOAD_CONFIRM_TITLE);

        for (int i = 0; i < 27; i++) {
            gui.setItem(i, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        gui.setItem(11, createItem(Material.COMPARATOR, GOLD + "Confirm Reload",
            "",
            WHITE + "Reload configuration?",
            "",
            GRAY + "This will reload config.yml"
        ));

        gui.setItem(15, createItem(Material.RED_DYE, RED + "Cancel",
            "",
            GRAY + "Click to cancel"
        ));

        player.openInventory(gui);
    }

    public void openHelpGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, HELP_TITLE);

        fillBackground(gui);

        gui.setItem(10, createItem(Material.ENDER_PEARL, GOLD + "/otunnel status",
            "",
            GRAY + "Check tunnel status",
            GRAY + "Shows version, uptime, errors"
        ));

        gui.setItem(11, createItem(Material.BARREL, GOLD + "/otunnel stats",
            "",
            GRAY + "Show detailed statistics",
            GRAY + "Uptime, restarts, PID"
        ));

        gui.setItem(12, createItem(Material.LIME_DYE, GREEN + "/otunnel start",
            "",
            GRAY + "Start the tunnel",
            GRAY + "Requires configured token"
        ));

        gui.setItem(13, createItem(Material.RED_DYE, RED + "/otunnel stop",
            "",
            GRAY + "Stop the tunnel",
            GRAY + "Graceful shutdown"
        ));

        gui.setItem(14, createItem(Material.YELLOW_DYE, YELLOW + "/otunnel restart",
            "",
            GRAY + "Restart the tunnel",
            GRAY + "Stop then start"
        ));

        gui.setItem(15, createItem(Material.PAPER, AQUA + "/otunnel update",
            "",
            GRAY + "Check and update",
            GRAY + "cloudflared binary"
        ));

        gui.setItem(16, createItem(Material.COMPARATOR, GOLD + "/otunnel reload",
            "",
            GRAY + "Reload configuration",
            GRAY + "config.yml"
        ));

        gui.setItem(22, createItem(Material.BOOK, GOLD + "\u00A7lTips",
            "",
            WHITE + "• Hover over items for details",
            WHITE + "• Click items to perform actions",
            WHITE + "• Use /otunnel for this GUI"
        ));

        gui.setItem(49, createItem(Material.ARROW, GOLD + "Back to Main Menu"));

        player.openInventory(gui);
    }

    private void fillBackground(Inventory gui) {
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (line != null && !line.isEmpty()) {
                        loreList.add(line);
                    }
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}
