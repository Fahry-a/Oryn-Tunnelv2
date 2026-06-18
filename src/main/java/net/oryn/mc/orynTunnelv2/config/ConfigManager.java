package net.oryn.mc.orynTunnelv2.config;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final OrynTunnelv2 plugin;
    private String token;
    private boolean autoUpdate;
    private int healthCheckInterval;
    private int maxRetries;

    private final List<String> validationErrors = new ArrayList<>();

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

        validate();
    }

    public void reload() {
        load();
    }

    private void validate() {
        validationErrors.clear();

        if (healthCheckInterval < 1) {
            validationErrors.add("health-check-interval must be at least 1 second");
            this.healthCheckInterval = 10;
        } else if (healthCheckInterval > 300) {
            validationErrors.add("health-check-interval must be at most 300 seconds");
            this.healthCheckInterval = 300;
        }

        if (maxRetries < 0) {
            validationErrors.add("max-retries cannot be negative");
            this.maxRetries = 5;
        } else if (maxRetries > 50) {
            validationErrors.add("max-retries must be at most 50");
            this.maxRetries = 50;
        }

        for (String error : validationErrors) {
            plugin.getLogger().warning("Config validation: " + error);
        }
    }

    public boolean isValid() {
        return validationErrors.isEmpty();
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
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
