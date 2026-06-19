package net.oryn.mc.orynTunnelv2.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ConfigManager {

    private final File dataFolder;
    private final Class<?> resourceSource;
    private final Logger logger;
    private FileConfiguration config;
    private String token;
    private boolean autoUpdate;
    private int healthCheckInterval;
    private int maxRetries;

    private final List<String> validationErrors = new ArrayList<>();

    public ConfigManager(File dataFolder, Class<?> resourceSource, Logger logger) {
        this.dataFolder = dataFolder;
        this.resourceSource = resourceSource;
        this.logger = logger;
        load();
    }

    public void load() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = resourceSource.getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    logger.warning("config.yml not found in module resources");
                }
            } catch (IOException e) {
                logger.warning("Failed to save default config: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

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
            logger.warning("Config validation: " + error);
        }
    }

    public boolean isValid() {
        return validationErrors.isEmpty();
    }

    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    public FileConfiguration getConfig() {
        return config;
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
