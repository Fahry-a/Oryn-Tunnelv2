package net.oryn.mc.orynTunnelv2.tunnel;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.log.LogManager;

import org.bukkit.scheduler.BukkitTask;

public class TunnelHealthChecker {

    private final OrynTunnelv2 plugin;
    private final CloudflaredManager cloudflaredManager;
    private final ConfigManager configManager;
    private final LogManager logManager;

    private BukkitTask healthCheckTask;
    private boolean enabled = false;

    public TunnelHealthChecker(OrynTunnelv2 plugin, CloudflaredManager cloudflaredManager,
                                ConfigManager configManager, LogManager logManager) {
        this.plugin = plugin;
        this.cloudflaredManager = cloudflaredManager;
        this.configManager = configManager;
        this.logManager = logManager;
    }

    public void start() {
        if (enabled) {
            return;
        }

        enabled = true;
        int interval = configManager.getHealthCheckInterval();
        long tickInterval = interval * 20L;

        healthCheckTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!cloudflaredManager.isAutoRestartEnabled()) {
                return;
            }

            if (!cloudflaredManager.isRunning()) {
                logManager.log("Health check: cloudflared is not running");
                plugin.getLogger().warning("Cloudflared is not running, attempting restart...");

                String token = configManager.getToken();
                if (token != null && !token.isEmpty() && !token.isBlank()) {
                    cloudflaredManager.restartTunnel(token);
                } else {
                    plugin.getLogger().warning("Cannot restart: token not configured");
                }
            }
        }, tickInterval, tickInterval);

        plugin.getLogger().info("Health checker started (interval: " + interval + "s)");
    }

    public void stop() {
        if (healthCheckTask != null) {
            healthCheckTask.cancel();
            healthCheckTask = null;
        }
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
