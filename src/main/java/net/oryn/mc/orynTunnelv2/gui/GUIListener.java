package net.oryn.mc.orynTunnelv2.gui;

import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class GUIListener implements Listener {

    private final JavaPlugin plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;
    private final TunnelHealthChecker healthChecker;
    private final TunnelGUI tunnelGUI;
    private final TunnelManager tunnelManager;

    private static final String PREFIX = ChatColor.GOLD + "[OrynTunnel] " + ChatColor.RESET;

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
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Tunnel is already running!");
                return;
            }

            String token = configManager.getToken();
            if (token == null || token.isBlank()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Token is not configured! Edit config.yml");
                return;
            }

            if (!cloudflaredManager.isBinaryExists()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Binary not found, downloading...");
                cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                    @Override
                    public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                        player.sendMessage(PREFIX + ChatColor.AQUA + "Download: " + percent + "%");
                    }

                    @Override
                    public void onComplete(boolean success) {
                        if (success) {
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                cloudflaredManager.resetAutoRestart();
                                cloudflaredManager.startTunnel(token);
                                healthChecker.start();
                                player.sendMessage(PREFIX + ChatColor.GREEN + "Tunnel started!");
                            });
                        } else {
                            player.sendMessage(PREFIX + ChatColor.RED + "Failed to download cloudflared!");
                        }
                    }
                });
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> cloudflaredManager.ensureBinary());
                return;
            }

            cloudflaredManager.resetAutoRestart();
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Tunnel started!");

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleStopConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();

            if (!cloudflaredManager.isRunning()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Tunnel is not running!");
                return;
            }

            healthChecker.stop();
            cloudflaredManager.stopTunnel();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Tunnel stopped!");

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleRestartConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();

            if (!cloudflaredManager.isRunning()) {
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Tunnel is not running! Use start.");
                return;
            }

            String token = configManager.getToken();
            if (token == null || token.isBlank()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Token is not configured!");
                return;
            }

            player.sendMessage(PREFIX + ChatColor.AQUA + "Restarting tunnel...");
            cloudflaredManager.stopTunnel();
            cloudflaredManager.resetAutoRestart();

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                cloudflaredManager.startTunnel(token);
                healthChecker.start();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Tunnel restarted!");
            }, 20L);

        } else if (slot == 15) {
            player.closeInventory();
        }
    }

    private void handleUpdateGUIClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 15) {
            player.closeInventory();
            player.sendMessage(PREFIX + ChatColor.AQUA + "Checking for updates...");

            cloudflaredManager.setDownloadCallback(new CloudflaredManager.DownloadCallback() {
                @Override
                public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                    player.sendMessage(PREFIX + ChatColor.AQUA + "Download: " + percent + "%");
                }

                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        player.sendMessage(PREFIX + ChatColor.GREEN + "Update complete!");
                    } else {
                        player.sendMessage(PREFIX + ChatColor.RED + "Update failed!");
                    }
                }
            });

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                String currentVersion = cloudflaredManager.getLocalVersion();
                String latestVersion = cloudflaredManager.getLatestVersion();

                if (latestVersion == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Could not fetch latest version");
                    return;
                }

                if (latestVersion.equals(currentVersion)) {
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Cloudflared is up to date (" + currentVersion + ")");
                    return;
                }

                player.sendMessage(PREFIX + ChatColor.AQUA + "New version: " + latestVersion + " (current: " + currentVersion + ")");
                cloudflaredManager.downloadBinary(latestVersion);
            });
        }
    }

    private void handleReloadConfirmClick(InventoryClickEvent event, Player player, int slot) {
        event.setCancelled(true);

        if (slot == 11) {
            player.closeInventory();
            if (tunnelManager != null) {
                tunnelManager.reloadConfig();
            } else {
                configManager.reload();
            }
            player.sendMessage(PREFIX + ChatColor.GREEN + "Configuration reloaded!");

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
}
