package net.oryn.mc.orynTunnelv2;

import net.oryn.mc.orynTunnelv2.command.TunnelCommand;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.GUIListener;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.listener.PlayerJoinListener;
import net.oryn.mc.orynTunnelv2.log.LogManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class OrynTunnelv2 extends JavaPlugin {

    private TunnelManager tunnelManager;

    @Override
    public void onEnable() {
        LogManager logManager = new LogManager(getDataFolder(), getLogger());
        ConfigManager configManager = new ConfigManager(getDataFolder(), getClass(), getLogger());

        if (!configManager.isValid()) {
            getLogger().warning("Config validation warnings found:");
            for (String error : configManager.getValidationErrors()) {
                getLogger().warning("  - " + error);
            }
        }

        logManager.setLogMaxSize(configManager.getLogMaxSize());

        tunnelManager = new TunnelManager(this, configManager, logManager);

        CloudflaredManager cloudflaredManager = tunnelManager.getCloudflaredManager();
        TunnelHealthChecker healthChecker = tunnelManager.getHealthChecker();

        TunnelGUI tunnelGUI = new TunnelGUI(this, cloudflaredManager, configManager);

        TunnelCommand tunnelCommand = new TunnelCommand(this, tunnelManager, tunnelGUI);
        PluginCommand cmd = getCommand("otunnel");
        if (cmd != null) {
            cmd.setExecutor(tunnelCommand);
            cmd.setTabCompleter(tunnelCommand);
        } else {
            getLogger().warning("Failed to register /otunnel command");
        }

        PlayerJoinListener playerJoinListener = new PlayerJoinListener(this, cloudflaredManager);
        getServer().getPluginManager().registerEvents(playerJoinListener, this);

        GUIListener guiListener = new GUIListener(this, cloudflaredManager, configManager, healthChecker, tunnelGUI);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getLogger().info("Oryn Tunnel v2 enabled");

        if (!configManager.isTokenConfigured()) {
            getLogger().warning("Token not configured! Use /otunnel or edit config.yml");
            return;
        }

        tunnelManager.startTunnelIfNeeded(true);
    }

    @Override
    public void onDisable() {
        getLogger().info("Oryn Tunnel v2 disabling...");

        if (tunnelManager != null) {
            tunnelManager.stopTunnel();
            tunnelManager.getLogManager().archive();
        }

        getLogger().info("Oryn Tunnel v2 disabled");
    }

    public ConfigManager getConfigManager() {
        return tunnelManager != null ? tunnelManager.getConfigManager() : null;
    }
}
