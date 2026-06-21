package net.oryn.mc.orynTunnelv2.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Notifies operators about available cloudflared updates on join.
 * Uses Adventure Component API for modern text rendering.
 */
public class PlayerJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final CloudflaredManager cloudflaredManager;

    private boolean updateNotificationSent = false;

    public PlayerJoinListener(JavaPlugin plugin, CloudflaredManager cloudflaredManager) {
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
                        player.sendMessage(
                            Component.text("[OrynTunnel] ", NamedTextColor.GOLD)
                                .append(Component.text("A new cloudflared update is available!", NamedTextColor.AQUA))
                        );
                        player.sendMessage(
                            Component.text("[OrynTunnel] ", NamedTextColor.GOLD)
                                .append(Component.text("Use ", NamedTextColor.GRAY)
                                    .append(Component.text("/otunnel update", NamedTextColor.AQUA))
                                    .append(Component.text(" to update.", NamedTextColor.GRAY)))
                        );
                        updateNotificationSent = true;
                    }
                });
            }
        });
    }

}
