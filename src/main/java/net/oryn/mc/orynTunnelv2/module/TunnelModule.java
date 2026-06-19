package net.oryn.mc.orynTunnelv2.module;

import net.oryn.mc.orynPlugins.module.ModuleContext;
import net.oryn.mc.orynPlugins.module.OrynModule;
import net.oryn.mc.orynTunnelv2.command.TunnelCommand;
import net.oryn.mc.orynTunnelv2.config.ConfigManager;
import net.oryn.mc.orynTunnelv2.gui.GUIListener;
import net.oryn.mc.orynTunnelv2.gui.TunnelGUI;
import net.oryn.mc.orynTunnelv2.listener.PlayerJoinListener;
import net.oryn.mc.orynTunnelv2.log.LogManager;
import net.oryn.mc.orynTunnelv2.tunnel.CloudflaredManager;
import net.oryn.mc.orynTunnelv2.tunnel.TunnelHealthChecker;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TunnelModule implements OrynModule {

    private ModuleContext context;
    private JavaPlugin hostPlugin;
    private ConfigManager configManager;
    private LogManager logManager;
    private CloudflaredManager cloudflaredManager;
    private TunnelHealthChecker healthChecker;
    private TunnelCommand tunnelCommand;

    @Override
    public String getName() {
        return "tunnel";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Cloudflare Tunnel plugin";
    }

    @Override
    public String getAuthor() {
        return "Fahry-a";
    }

    @Override
    public boolean onLoad(ModuleContext context) {
        this.context = context;
        this.hostPlugin = context.getHostPlugin();

        logManager = new LogManager(context.getModuleDataFolder(), context.getLogger());
        configManager = new ConfigManager(context.getModuleDataFolder(), TunnelModule.class, context.getLogger());

        if (!configManager.isValid()) {
            context.getLogger().warning("Config validation warnings found:");
            for (String error : configManager.getValidationErrors()) {
                context.getLogger().warning("  - " + error);
            }
        }

        cloudflaredManager = new CloudflaredManager(hostPlugin, context.getModuleDataFolder(), logManager);
        healthChecker = new TunnelHealthChecker(hostPlugin, cloudflaredManager, configManager, logManager);

        TunnelGUI tunnelGUI = new TunnelGUI(hostPlugin, cloudflaredManager, configManager);
        tunnelCommand = new TunnelCommand(hostPlugin, cloudflaredManager, configManager, healthChecker, tunnelGUI);

        PlayerJoinListener playerJoinListener = new PlayerJoinListener(hostPlugin, cloudflaredManager);
        hostPlugin.getServer().getPluginManager().registerEvents(playerJoinListener, hostPlugin);

        GUIListener guiListener = new GUIListener(hostPlugin, cloudflaredManager, configManager, healthChecker, tunnelGUI);
        hostPlugin.getServer().getPluginManager().registerEvents(guiListener, hostPlugin);

        context.getLogger().info("Tunnel module loaded");
        return true;
    }

    @Override
    public void onEnable() {
        if (!configManager.isTokenConfigured()) {
            context.getLogger().warning("Token not configured! Use /oryn module tunnel or edit config.yml");
            return;
        }

        if (configManager.isAutoUpdate()) {
            hostPlugin.getServer().getScheduler().runTaskAsynchronously(hostPlugin, () -> {
                cloudflaredManager.checkAndUpdate();
                startTunnelIfNeeded();
            });
        } else {
            startTunnelIfNeeded();
        }

        context.getLogger().info("Tunnel module enabled");
    }

    private void startTunnelIfNeeded() {
        hostPlugin.getServer().getScheduler().runTask(hostPlugin, () -> {
            if (!cloudflaredManager.ensureBinary()) {
                context.getLogger().severe("Could not ensure cloudflared binary. Tunnel not started.");
                return;
            }

            String token = configManager.getToken();
            cloudflaredManager.startTunnel(token);
            healthChecker.start();
        });
    }

    @Override
    public void onDisable() {
        context.getLogger().info("Tunnel module disabling...");

        healthChecker.stop();
        cloudflaredManager.stopTunnel();
        logManager.archive();

        context.getLogger().info("Tunnel module disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        return tunnelCommand.onModuleCommand(sender, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return tunnelCommand.onModuleTabComplete(sender, label, args);
    }
}
