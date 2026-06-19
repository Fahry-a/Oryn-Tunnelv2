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
import net.oryn.mc.orynTunnelv2.tunnel.TunnelManager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TunnelModule implements OrynModule {

    private ModuleContext context;
    private JavaPlugin hostPlugin;
    private TunnelManager tunnelManager;

    @Override
    public String getName() {
        return "tunnel";
    }

    @Override
    public String getVersion() {
        return "1.1";
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

        ConfigManager configManager = new ConfigManager(
            context.getModuleDataFolder(),
            TunnelModule.class,
            context.getLogger()
        );

        LogManager logManager = new LogManager(
            context.getModuleDataFolder(),
            context.getLogger(),
            configManager.getLogMaxSize()
        );

        tunnelManager = new TunnelManager(hostPlugin, configManager, logManager);

        CloudflaredManager cloudflaredManager = tunnelManager.getCloudflaredManager();
        TunnelHealthChecker healthChecker = tunnelManager.getHealthChecker();

        TunnelGUI tunnelGUI = new TunnelGUI(hostPlugin, cloudflaredManager, configManager);

        TunnelCommand tunnelCommand = new TunnelCommand(hostPlugin, tunnelManager, tunnelGUI, "/oryn module tunnel help");

        PlayerJoinListener playerJoinListener = new PlayerJoinListener(hostPlugin, cloudflaredManager);
        hostPlugin.getServer().getPluginManager().registerEvents(playerJoinListener, hostPlugin);

        GUIListener guiListener = new GUIListener(hostPlugin, cloudflaredManager, configManager, healthChecker, tunnelGUI);
        hostPlugin.getServer().getPluginManager().registerEvents(guiListener, hostPlugin);

        context.getLogger().info("Tunnel module loaded");
        return true;
    }

    @Override
    public void onEnable() {
        if (!tunnelManager.getConfigManager().isTokenConfigured()) {
            context.getLogger().warning("Token not configured! Use /oryn module tunnel or edit config.yml");
            return;
        }

        tunnelManager.startTunnelIfNeeded(true);
        context.getLogger().info("Tunnel module enabled");
    }

    @Override
    public void onDisable() {
        context.getLogger().info("Tunnel module disabling...");

        tunnelManager.stopTunnel();
        tunnelManager.getLogManager().archive();

        context.getLogger().info("Tunnel module disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        return tunnelManager.getCloudflaredManager() != null
            ? new TunnelCommand(hostPlugin, tunnelManager,
                new TunnelGUI(hostPlugin, tunnelManager.getCloudflaredManager(), tunnelManager.getConfigManager()),
                "/oryn module tunnel help")
                .onModuleCommand(sender, label, args)
            : false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return List.of();
    }
}
