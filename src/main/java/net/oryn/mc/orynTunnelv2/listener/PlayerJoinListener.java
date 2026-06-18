package net.oryn.mc.orynTunnelv2.listener;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final OrynTunnelv2 plugin;
    private final CloudflaredManager cloudflaredManager;

    private boolean updateNotificationSent = false;

    public PlayerJoinListener(OrynTunnelv2 plugin, CloudflaredManager cloudflaredManager) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPermission("otunnel.admin")) {
            return;
        }

        if (updateNotificationSent) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (cloudflaredManager.isUpdateAvailable()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.GOLD + "[OrynTunnel] " + ChatColor.AQUA + "A new cloudflared update is available!");
                        player.sendMessage(ChatColor.GOLD + "[OrynTunnel] " + ChatColor.GRAY + "Use " + ChatColor.AQUA + "/otunnel update" + ChatColor.GRAY + " to update.");
                        updateNotificationSent = true;
                    }
                });
            }
        });
    }

    public void resetNotification() {
        updateNotificationSent = false;
    }
}
