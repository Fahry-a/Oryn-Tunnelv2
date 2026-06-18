package net.oryn.mc.orynTunnelv2;

import net.oryn.mc.orynTunnelv2.command.TunnelCommand;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.log.LogManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrynTunnelv2 extends JavaPlugin {

    private ConfigManager configManager;
    private LogManager logManager;
    private CloudflaredManager cloudflaredManager;
    private TunnelHealthChecker healthChecker;
    private TunnelCommand tunnelCommand;

    @Override
    public void onEnable() {
        logManager = new LogManager(this);
        configManager = new ConfigManager(this);
        cloudflaredManager = new CloudflaredManager(this, logManager);
        healthChecker = new TunnelHealthChecker(this, cloudflaredManager, configManager, logManager);

        tunnelCommand = new TunnelCommand(this, cloudflaredManager, configManager, logManager, healthChecker);
        PluginCommand cmd = getCommand("otunnel");
        if (cmd != null) {
            cmd.setExecutor(tunnelCommand);
            cmd.setTabCompleter(tunnelCommand);
        }

        getLogger().info("Oryn Tunnel v2 enabled");

        if (!configManager.isTokenConfigured()) {
            getLogger().warning("Token not configured! Use /otunnel or edit config.yml");
            return;
        }

        if (configManager.isAutoUpdate()) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                cloudflaredManager.checkAndUpdate();
                startTunnelIfNeeded();
            });
        } else {
            startTunnelIfNeeded();
        }
    }

    private void startTunnelIfNeeded() {
        getServer().getScheduler().runTask(this, () -> {
            if (!cloudflaredManager.ensureBinary()) {
                getLogger().severe("Could not ensure cloudflared binary. Tunnel not started.");
                return;
            }

            String token = configManager.getToken();
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Oryn Tunnel v2 disabling...");

        healthChecker.stop();
        cloudflaredManager.stopTunnel();
        logManager.archive();

        getLogger().info("Oryn Tunnel v2 disabled");
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
