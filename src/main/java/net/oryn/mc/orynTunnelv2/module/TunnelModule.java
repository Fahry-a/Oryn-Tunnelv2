package net.oryn.mc.orynTunnelv2.module;

import net.oryn.mc.orynPlugins.module.ModuleContext;
import net.oryn.mc.orynPlugins.module.ModuleInfo;
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

@ModuleInfo(
    name = "tunnel",
    version = "1.2.0",
    description = "Cloudflare Tunnel plugin - manages cloudflared binary and tunnel connections",
    author = "Fahry-a"
)
public class TunnelModule implements OrynModule {

    private ModuleContext context;
    private JavaPlugin hostPlugin;
    private TunnelManager tunnelManager;
    private TunnelCommand tunnelCommand;
    private TunnelGUI tunnelGUI;
    private PlayerJoinListener playerJoinListener;
    private GUIListener guiListener;

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

        tunnelGUI = new TunnelGUI(hostPlugin, cloudflaredManager, configManager);

        tunnelCommand = new TunnelCommand(hostPlugin, tunnelManager, tunnelGUI, "/oryn module tunnel help");

        // Register event listeners - auto-unregistered on disable
        playerJoinListener = new PlayerJoinListener(hostPlugin, cloudflaredManager);
        context.registerEvents(playerJoinListener);

        guiListener = new GUIListener(hostPlugin, cloudflaredManager, configManager, healthChecker, tunnelGUI, tunnelManager);
        context.registerEvents(guiListener);

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
    public void onReload() {
        context.getLogger().info("Reloading tunnel configuration...");
        tunnelManager.reloadConfig();
        context.getLogger().info("Tunnel configuration reloaded");
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, String[] args) {
        return tunnelCommand != null ? tunnelCommand.onModuleCommand(sender, label, args) : false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
        return tunnelCommand != null ? tunnelCommand.onModuleTabComplete(sender, label, args) : List.of();
    }
}
