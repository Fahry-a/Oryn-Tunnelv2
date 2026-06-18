package net.oryn.mc.orynTunnelv2.config;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final OrynTunnelv2 plugin;
    private String token;
    private boolean autoUpdate;
    private int healthCheckInterval;
    private int maxRetries;

    public ConfigManager(OrynTunnelv2 plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.token = config.getString("token", "");
        this.autoUpdate = config.getBoolean("auto-update", true);
        this.healthCheckInterval = config.getInt("health-check-interval", 10);
        this.maxRetries = config.getInt("max-retries", 5);
    }

    public void reload() {
        load();
    }

    public String getToken() {
        return token;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public int getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isTokenConfigured() {
        return token != null && !token.isEmpty() && !token.isBlank();
    }
}
