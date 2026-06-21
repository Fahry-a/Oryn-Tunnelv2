package net.oryn.mc.orynTunnelv2.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles GUI click events for the tunnel management interface.
 * Uses Adventure Component API for modern text rendering.
 */
public class GUIListener implements Listener {

    private final JavaPlugin plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;
    private final TunnelHealthChecker healthChecker;
    private final TunnelGUI tunnelGUI;
    private final TunnelManager tunnelManager;

    private static Component prefix() {
        return Component.text("[OrynTunnel] ", NamedTextColor.GOLD);
    }

    public GUIListener(JavaPlugin plugin, CloudflaredManager cloudflaredManager,
                       ConfigManager configManager, TunnelHealthChecker healthChecker,
                       TunnelGUI tunnelGUI) {
        this(plugin, cloudflaredManager, configManager, healthChecker, tunnelGUI, null);
    }

    public GUIListener(JavaPlugin plugin, CloudflaredManager cloudflaredManager,
                       ConfigManager configManager, TunnelHealthChecker healthChecker,
                       TunnelGUI tunnelGUI, TunnelManager tunnelManager) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
        this.configManager = configManager;
        this.healthChecker = healthChecker;
        this.tunnelGUI = tunnelGUI;
        this.tunnelManager = tunnelManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory == null) {
            return;
        }

        String title = player.getOpenInventory().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().name().contains("STAINED_GLASS_PANE")) {
            event.setCancelled(true);
            return;
        }

        if (title.equals(TunnelGUI.MAIN_MENU_TITLE)) {
            handleMainMenuClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.STATUS_TITLE)) {
            handleStatusGUIClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.STATS_TITLE)) {
            handleStatsGUIClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.START_CONFIRM_TITLE)) {
            handleStartConfirmClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.STOP_CONFIRM_TITLE)) {
            handleStopConfirmClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.RESTART_CONFIRM_TITLE)) {
            handleRestartConfirmClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.UPDATE_TITLE)) {
            handleUpdateGUIClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.RELOAD_CONFIRM_TITLE)) {
            handleReloadConfirmClick(event, player, event.getRawSlot());
        } else if (title.equals(TunnelGUI.HELP_TITLE)) {
            handleHelpGUIClick(event, player, event.getRawSlot());
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        switch (slot) {
            case 10:
                tunnelGUI.openStatusGUI(player);
                break;
            case 11:
                tunnelGUI.openStatsGUI(player);
                break;
            case 13:
                tunnelGUI.openStartConfirmGUI(player);
                break;
            case 14:
                tunnelGUI.openStopConfirmGUI(player);
                break;
            case 15:
                tunnelGUI.openRestartConfirmGUI(player);
                break;
            case 28:
                tunnelGUI.openUpdateGUI(player);
                break;
            case 29:
                tunnelGUI.openReloadConfirmGUI(player);
                break;
            case 31:
                tunnelGUI.openHelpGUI(player);
                break;
            case 49:
                player.closeInventory();
                break;
        }
    }

    private void handleStatusGUIClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);
        if (slot == 49) {
            tunnelGUI.openMainMenu(player);
        }
    }

    private void handleStatsGUIClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);
        if (slot == 49) {
            tunnelGUI.openMainMenu(player);
        }
    }

    private void handleStartConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();

            if (cloudflaredManager.isRunning()) {
                sendSafe(player, prefix().append(Component.text("Tunnel is already running!", NamedTextColor.YELLOW)));
                return;
            }

            String token = configManager.getToken();
            if (token == null || token.isBlank()) {
                sendSafe(player, prefix().append(Component.text("Token is not configured! Edit config.yml", NamedTextColor.RED)));
                return;
            }

            if (!cloudflaredManager.isBinaryExists()) {
                sendSafe(player, prefix().append(Component.text("Binary not found, downloading...", NamedTextColor.YELLOW)));
                cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                    @Override
                    public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                        sendSafe(player, prefix().append(Component.text("Download: " + percent + "%", NamedTextColor.AQUA)));
                    }

                    @Override
                    public void onComplete(boolean success) {
                        if (success) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                cloudflaredManager.resetAutoRestart();
                                cloudflaredManager.startTunnel(token);
                                healthChecker.start();
                                sendSafe(player, prefix().append(Component.text("Tunnel started!", NamedTextColor.GREEN)));
                            });
                        } else {
                            sendSafe(player, prefix().append(Component.text("Failed to download cloudflared!", NamedTextColor.RED)));
                        }
                    }
                });
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> cloudflaredManager.ensureBinary());
                return;
            }

            cloudflaredManager.resetAutoRestart();
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
            sendSafe(player, prefix().append(Component.text("Tunnel started!", NamedTextColor.GREEN)));

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleStopConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();

            if (!cloudflaredManager.isRunning()) {
                sendSafe(player, prefix().append(Component.text("Tunnel is not running!", NamedTextColor.YELLOW)));
                return;
            }

            healthChecker.stop();
            cloudflaredManager.stopTunnel();
            sendSafe(player, prefix().append(Component.text("Tunnel stopped!", NamedTextColor.GREEN)));

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleRestartConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();

            if (!cloudflaredManager.isRunning()) {
                sendSafe(player, prefix().append(Component.text("Tunnel is not running! Use start.", NamedTextColor.YELLOW)));
                return;
            }

            String token = configManager.getToken();
            if (token == null || token.isBlank()) {
                sendSafe(player, prefix().append(Component.text("Token is not configured!", NamedTextColor.RED)));
                return;
            }

            sendSafe(player, prefix().append(Component.text("Restarting tunnel...", NamedTextColor.AQUA)));
            cloudflaredManager.stopTunnel();
            cloudflaredManager.resetAutoRestart();

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                cloudflaredManager.startTunnel(token);
                healthChecker.start();
                sendSafe(player, prefix().append(Component.text("Tunnel restarted!", NamedTextColor.GREEN)));
            }, 20L);

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleUpdateGUIClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 15) {
            player.closeInventory();
            sendSafe(player, prefix().append(Component.text("Checking for updates...", NamedTextColor.AQUA)));

            cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    sendSafe(player, prefix().append(Component.text("Download: " + percent + "%", NamedTextColor.AQUA)));
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        sendSafe(player, prefix().append(Component.text("Update complete!", NamedTextColor.GREEN)));
                    } else {
                        sendSafe(player, prefix().append(Component.text("Update failed!", NamedTextColor.RED)));
                    }
                }
            });

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String currentVersion = cloudflaredManager.getLocalVersion();
                String latestVersion = cloudflaredManager.getLatestVersion();

                if (latestVersion == null) {
                    sendSafe(player, prefix().append(Component.text("Could not fetch latest version", NamedTextColor.RED)));
                    return;
                }

                if (latestVersion.equals(currentVersion)) {
                    sendSafe(player, prefix().append(Component.text("Cloudflared is up to date (" + currentVersion + ")", NamedTextColor.GREEN)));
                    return;
                }

                sendSafe(player, prefix().append(Component.text("New version: " + latestVersion + " (current: " + currentVersion + ")", NamedTextColor.AQUA)));
                cloudflaredManager.downloadBinary(latestVersion);
            });
        }
    }

    private void handleReloadConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();
            try {
                if (tunnelManager != null) {
                    tunnelManager.reloadConfig();
                } else {
                    configManager.reload();
                }
                sendSafe(player, prefix().append(Component.text("Configuration reloaded!", NamedTextColor.GREEN)));
            } catch (Exception e) {
                sendSafe(player, prefix().append(Component.text("Failed to reload configuration: " + e.getMessage(), NamedTextColor.RED)));
                plugin.getLogger().warning("Config reload failed: " + e.getMessage());
            }

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleHelpGUIClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);
        if (slot == 49) {
            tunnelGUI.openMainMenu(player);
        }
    }

    private void sendSafe(Player player, Component message) {
        if (player.isOnline()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(message);
                }
            });
        }
    }

}
