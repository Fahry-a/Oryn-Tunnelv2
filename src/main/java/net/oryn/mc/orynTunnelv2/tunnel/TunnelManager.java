package net.oryn.mc.orynTunnelv2.tunnel;

import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.log.LogManager;

import org.bukkit.plugin.java.JavaPlugin;

public class TunnelManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final LogManager logManager;
    private final CloudflaredManager cloudflaredManager;
    private final TunnelHealthChecker healthChecker;

    public TunnelManager(JavaPlugin plugin, ConfigManager configManager, LogManager logManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logManager = logManager;
        this.cloudflaredManager = new CloudflaredManager(plugin, plugin.getDataFolder(), logManager);
        this.healthChecker = new TunnelHealthChecker(plugin, cloudflaredManager, configManager, logManager);
    }

    public void startTunnelIfNeeded() {
        startTunnelIfNeeded(false);
    }

    public void startTunnelIfNeeded(boolean autoUpdate) {
        if (autoUpdate && configManager.isAutoUpdate()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                cloudflaredManager.checkAndUpdate(configManager.getCloudflaredVersion());
                startTunnelOnMain();
            });
        } else {
            startTunnelOnMain();
        }
    }

    private void startTunnelOnMain() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!cloudflaredManager.ensureBinary(configManager.getCloudflaredVersion())) {
                plugin.getLogger().severe("Could not ensure cloudflared binary. Tunnel not started.");
                return;
            }

            String token = configManager.getToken();
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
        });
    }

    public void stopTunnel() {
        healthChecker.stop();
        cloudflaredManager.stopTunnel();
    }

    public void reloadConfig() {
        boolean tokenChanged = configManager.reload();
        if (tokenChanged && cloudflaredManager.isRunning()) {
            plugin.getLogger().info("Token changed, restarting tunnel...");
            cloudflaredManager.stopTunnel();
            startTunnelOnMain();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public CloudflaredManager getCloudflaredManager() {
        return cloudflaredManager;
    }

    public TunnelHealthChecker getHealthChecker() {
        return healthChecker;
    }
}
