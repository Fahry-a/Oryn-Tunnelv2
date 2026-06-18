package net.oryn.mc.orynTunnelv2.tunnel;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;
import net.oryn.mc.orynTunnelv2.log.LogManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitTask;

public class CloudflaredManager {

    private final OrynTunnelv2 plugin;
    private final File binDir;
    private final File binaryFile;
    private final File versionFile;
    private final LogManager logManager;

    private Process cloudflaredProcess;
    private BukkitTask restartTask;
    private int retryCount;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean autoRestartEnabled = true;

    private static final String GITHUB_API_URL = "https://api.github.com/repos/cloudflare/cloudflared/releases/latest";
    private static final String BINARY_NAME = "cloudflared";

    public CloudflaredManager(OrynTunnelv2 plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
        this.binDir = new File(plugin.getDataFolder(), "bin");
        this.binaryFile = new File(binDir, BINARY_NAME);
        this.versionFile = new File(binDir, "version.txt");
        this.retryCount = 0;

        if (!binDir.exists()) {
            binDir.mkdirs();
        }
    }

    public boolean isBinaryExists() {
        return binaryFile.exists() && binaryFile.canExecute();
    }

    public String getLocalVersion() {
        if (!versionFile.exists()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(versionFile.toPath())).trim();
        } catch (IOException e) {
            return null;
        }
    }

    private void saveLocalVersion(String version) {
        try {
            Files.write(versionFile.toPath(), version.getBytes());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save version file: " + e.getMessage());
        }
    }

    public String getLatestVersion() {
        try {
            URL url = new URL(GITHUB_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String json = response.toString();
            int tagIndex = json.indexOf("\"tag_name\":\"");
            if (tagIndex == -1) {
                return null;
            }
            int start = tagIndex + "\"tag_name\":\"".length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to fetch latest version", e);
            return null;
        }
    }

    public String getDownloadUrl(String version) {
        String os = "linux";
        String arch = System.getProperty("os.arch").toLowerCase();
        String archName;

        if (arch.equals("amd64") || arch.equals("x86_64")) {
            archName = "amd64";
        } else if (arch.equals("aarch64") || arch.equals("arm64")) {
            archName = "arm64";
        } else {
            archName = "amd64";
        }

        return "https://github.com/cloudflare/cloudflared/releases/download/" + version + "/cloudflared-" + os + "-" + archName;
    }

    public boolean downloadBinary(String version) {
        String downloadUrl = getDownloadUrl(version);
        if (downloadUrl == null) {
            return false;
        }

        plugin.getLogger().info("Downloading cloudflared " + version + "...");
        logManager.log("Starting download: " + downloadUrl);

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "OrynTunnelv2/1.0");

            if (conn.getResponseCode() != 200) {
                plugin.getLogger().severe("Download failed with HTTP " + conn.getResponseCode());
                return false;
            }

            File tempFile = new File(binDir, BINARY_NAME + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                int lastPercent = 0;
                long contentLength = conn.getContentLengthLong();

                try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;

                        if (contentLength > 0) {
                            int percent = (int) ((totalBytes * 100) / contentLength);
                            if (percent >= lastPercent + 10) {
                                lastPercent = percent;
                                plugin.getLogger().info("Download progress: " + percent + "%");
                            }
                        }
                    }
                }
            }

            tempFile.setExecutable(true);
            tempFile.renameTo(binaryFile);
            saveLocalVersion(version);

            plugin.getLogger().info("Download complete: cloudflared " + version);
            logManager.log("Download complete: cloudflared " + version);
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download cloudflared", e);
            return false;
        }
    }

    public boolean checkAndUpdate() {
        plugin.getLogger().info("Checking for cloudflared updates...");
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            plugin.getLogger().warning("Could not fetch latest version from GitHub");
            return false;
        }

        String localVersion = getLocalVersion();
        if (latestVersion.equals(localVersion)) {
            plugin.getLogger().info("Cloudflared is up to date (" + localVersion + ")");
            return true;
        }

        plugin.getLogger().info("New version available: " + latestVersion + " (current: " + localVersion + ")");
        return downloadBinary(latestVersion);
    }

    public boolean ensureBinary() {
        if (isBinaryExists()) {
            plugin.getLogger().info("Cloudflared binary found");
            return true;
        }

        plugin.getLogger().info("Cloudflared binary not found, downloading...");
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            plugin.getLogger().severe("Could not fetch latest version from GitHub");
            return false;
        }

        return downloadBinary(latestVersion);
    }

    public void startTunnel(String token) {
        if (running.get()) {
            plugin.getLogger().warning("Tunnel is already running");
            return;
        }

        if (!isBinaryExists()) {
            plugin.getLogger().severe("Cloudflared binary not found");
            return;
        }

        if (token == null || token.isEmpty() || token.isBlank()) {
            plugin.getLogger().severe("Token is not configured!");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                binaryFile.getAbsolutePath(),
                "tunnel",
                "run",
                "--token", token
            );
            pb.redirectErrorStream(true);

            cloudflaredProcess = pb.start();
            running.set(true);
            retryCount = 0;

            plugin.getLogger().info("Cloudflared tunnel started");
            logManager.log("Cloudflared tunnel started with PID: " + cloudflaredProcess.pid());

            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(cloudflaredProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logManager.log(line);
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        plugin.getLogger().warning("Error reading cloudflared output: " + e.getMessage());
                    }
                }
            });
            logThread.setDaemon(true);
            logThread.start();

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start cloudflared", e);
            running.set(false);
        }
    }

    public void stopTunnel() {
        autoRestartEnabled = false;

        if (cloudflaredProcess != null && cloudflaredProcess.isAlive()) {
            plugin.getLogger().info("Stopping cloudflared tunnel...");
            logManager.log("Stopping cloudflared tunnel");

            cloudflaredProcess.destroy();

            try {
                boolean terminated = cloudflaredProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!terminated) {
                    cloudflaredProcess.destroyForcibly();
                    plugin.getLogger().warning("Cloudflared forcefully terminated");
                }
            } catch (InterruptedException e) {
                cloudflaredProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }

            cloudflaredProcess = null;
            running.set(false);
            plugin.getLogger().info("Cloudflared tunnel stopped");
            logManager.log("Cloudflared tunnel stopped");
        }

        if (restartTask != null) {
            restartTask.cancel();
            restartTask = null;
        }
    }

    public void restartTunnel(String token) {
        int maxRetries = plugin.getConfigManager().getMaxRetries();

        if (maxRetries > 0 && retryCount >= maxRetries) {
            plugin.getLogger().severe("Max restart retries (" + maxRetries + ") reached. Auto-restart disabled.");
            logManager.log("Max restart retries reached, auto-restart disabled");
            autoRestartEnabled = false;
            running.set(false);
            return;
        }

        retryCount++;
        plugin.getLogger().warning("Restarting cloudflared (attempt " + retryCount + "/" + maxRetries + ")");
        logManager.log("Restarting cloudflared (attempt " + retryCount + "/" + maxRetries + ")");

        cloudflaredProcess = null;
        running.set(false);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> startTunnel(token), 20L);
    }

    public boolean isRunning() {
        return running.get() && cloudflaredProcess != null && cloudflaredProcess.isAlive();
    }

    public boolean isAutoRestartEnabled() {
        return autoRestartEnabled;
    }

    public void resetAutoRestart() {
        autoRestartEnabled = true;
        retryCount = 0;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Process getProcess() {
        return cloudflaredProcess;
    }
}
