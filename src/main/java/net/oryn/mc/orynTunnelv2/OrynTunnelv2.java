package net.oryn.mc.orynTunnelv2;

import net.oryn.mc.orynTunnelv2.command.TunnelCommand;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.GUIListener;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.listener.PlayerJoinListener;
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
    private PlayerJoinListener playerJoinListener;

    @Override
    public void onEnable() {
        logManager = new LogManager(this);
        configManager = new ConfigManager(this);

        if (!configManager.isValid()) {
            getLogger().warning("Config validation warnings found:");
            for (String error : configManager.getValidationErrors()) {
                getLogger().warning("  - " + error);
            }
        }

        cloudflaredManager = new CloudflaredManager(this, logManager);
        healthChecker = new TunnelHealthChecker(this, cloudflaredManager, configManager, logManager);

        TunnelGUI tunnelGUI = new TunnelGUI(this, cloudflaredManager, configManager);

        TunnelCommand tunnelCommand = new TunnelCommand(this, cloudflaredManager, configManager, healthChecker, tunnelGUI);
        PluginCommand cmd = getCommand("otunnel");
        if (cmd != null) {
            cmd.setExecutor(tunnelCommand);
            cmd.setTabCompleter(tunnelCommand);
        }

        playerJoinListener = new PlayerJoinListener(this, cloudflaredManager);
        getServer().getPluginManager().registerEvents(playerJoinListener, this);

        GUIListener guiListener = new GUIListener(this, cloudflaredManager, configManager, healthChecker, tunnelGUI);
        getServer().getPluginManager().registerEvents(guiListener, this);

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

    public void resetUpdateNotification() {
        if (playerJoinListener != null) {
            playerJoinListener.resetNotification();
        }
    }
}
