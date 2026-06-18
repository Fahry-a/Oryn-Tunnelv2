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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.bukkit.scheduler.BukkitTask;

public class CloudflaredManager {

    private final OrynTunnelv2 plugin;
    private final File binDir;
    private final File binaryFile;
    private final File versionFile;
    private final File checksumFile;
    private final LogManager logManager;

    private Process cloudflaredProcess;
    private BukkitTask restartTask;
    private int retryCount;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean autoRestartEnabled = true;

    private long startTime;
    private int totalRestarts;
    private String lastError;

    private DownloadCallback downloadCallback;

    private static final String GITHUB_API_URL = "https://api.github.com/repos/cloudflare/cloudflared/releases/latest";
    private static final String BINARY_NAME = "cloudflared";

    public interface DownloadCallback {
        void onProgress(int percent, long bytesDownloaded, long totalBytes);
        void onComplete(boolean success);
    }

    public CloudflaredManager(OrynTunnelv2 plugin, LogManager logManager) {
        this.plugin = plugin;
        this.logManager = logManager;
        this.binDir = new File(plugin.getDataFolder(), "bin");
        this.binaryFile = new File(binDir, BINARY_NAME);
        this.versionFile = new File(binDir, "version.txt");
        this.checksumFile = new File(binDir, "checksum.txt");
        this.retryCount = 0;
        this.startTime = 0;
        this.totalRestarts = 0;

        if (!binDir.exists() && !binDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create bin directory");
        }
    }

    public void setDownloadCallback(DownloadCallback callback) {
        this.downloadCallback = callback;
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
            URL url = URI.create(GITHUB_API_URL).toURL();
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

    private String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String fetchChecksumFromGitHub(String version) {
        try {
            String os = "linux";
            String arch = System.getProperty("os.arch").toLowerCase();
            String archName = arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : "amd64";

            String checksumUrl = "https://github.com/cloudflare/cloudflared/releases/download/" + version + "/cloudflared-" + os + "-" + archName + ".sha256";
            URL url = URI.create(checksumUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 1) {
                        return parts[0].trim();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "Could not fetch checksum from GitHub", e);
        }
        return null;
    }

    public boolean downloadBinary(String version) {
        String downloadUrl = getDownloadUrl(version);
        if (downloadUrl == null) {
            return false;
        }

        plugin.getLogger().info("Downloading cloudflared " + version + "...");
        logManager.log("Starting download: " + downloadUrl);

        try {
            URL url = URI.create(downloadUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "OrynTunnelv2/1.0");

            if (conn.getResponseCode() != 200) {
                lastError = "Download failed with HTTP " + conn.getResponseCode();
                plugin.getLogger().severe(lastError);
                return false;
            }

            File tempFile = new File(binDir, BINARY_NAME + ".tmp");
            long contentLength = conn.getContentLengthLong();

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                int lastPercent = 0;

                try (BufferedInputStream bis = new BufferedInputStream(conn.getInputStream())) {
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;

                        if (contentLength > 0) {
                            int percent = (int) ((totalBytes * 100) / contentLength);
                            if (percent >= lastPercent + 10) {
                                lastPercent = percent;
                                plugin.getLogger().info("Download progress: " + percent + "%");
                                if (downloadCallback != null) {
                                    downloadCallback.onProgress(percent, totalBytes, contentLength);
                                }
                            }
                        }
                    }
                }
            }

            String localChecksum = calculateSHA256(tempFile);
            String remoteChecksum = fetchChecksumFromGitHub(version);

            if (remoteChecksum != null && !remoteChecksum.equalsIgnoreCase(localChecksum)) {
                tempFile.delete();
                lastError = "Checksum mismatch! Expected: " + remoteChecksum + ", Got: " + localChecksum;
                plugin.getLogger().severe(lastError);
                logManager.log("Checksum verification failed: " + lastError);
                return false;
            }

            if (remoteChecksum != null) {
                Files.write(checksumFile.toPath(), remoteChecksum.getBytes());
                plugin.getLogger().info("SHA256 checksum verified: " + localChecksum);
                logManager.log("SHA256 checksum verified: " + localChecksum);
            } else {
                plugin.getLogger().warning("Checksum not available from GitHub, skipping verification");
            }

            if (!tempFile.setExecutable(true)) {
                plugin.getLogger().warning("Failed to set executable permission");
            }

            if (binaryFile.exists() && !binaryFile.delete()) {
                plugin.getLogger().warning("Failed to delete old binary");
            }

            if (!tempFile.renameTo(binaryFile)) {
                lastError = "Failed to rename temporary file to binary";
                plugin.getLogger().severe(lastError);
                return false;
            }

            saveLocalVersion(version);

            plugin.getLogger().info("Download complete: cloudflared " + version);
            logManager.log("Download complete: cloudflared " + version);

            if (downloadCallback != null) {
                downloadCallback.onComplete(true);
            }
            lastError = null;
            return true;

        } catch (IOException | NoSuchAlgorithmException e) {
            lastError = "Failed to download cloudflared: " + e.getMessage();
            plugin.getLogger().log(Level.SEVERE, lastError, e);
            logManager.log("Download failed: " + lastError);
            if (downloadCallback != null) {
                downloadCallback.onComplete(false);
            }
            return false;
        }
    }

    public void checkAndUpdate() {
        plugin.getLogger().info("Checking for cloudflared updates...");
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            lastError = "Could not fetch latest version from GitHub";
            plugin.getLogger().warning(lastError);
            return;
        }

        String localVersion = getLocalVersion();
        if (latestVersion.equals(localVersion)) {
            plugin.getLogger().info("Cloudflared is up to date (" + localVersion + ")");
            return;
        }

        plugin.getLogger().info("New version available: " + latestVersion + " (current: " + localVersion + ")");
        downloadBinary(latestVersion);
    }

    public boolean isUpdateAvailable() {
        String latestVersion = getLatestVersion();
        String localVersion = getLocalVersion();
        if (latestVersion == null || localVersion == null) {
            return false;
        }
        return !latestVersion.equals(localVersion);
    }

    public boolean ensureBinary() {
        if (isBinaryExists()) {
            plugin.getLogger().info("Cloudflared binary found");
            return true;
        }

        plugin.getLogger().info("Cloudflared binary not found, downloading...");
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            lastError = "Could not fetch latest version from GitHub";
            plugin.getLogger().severe(lastError);
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
            lastError = "Cloudflared binary not found";
            plugin.getLogger().severe(lastError);
            return;
        }

        if (token == null || token.isBlank()) {
            lastError = "Token is not configured!";
            plugin.getLogger().severe(lastError);
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
            startTime = System.currentTimeMillis();
            lastError = null;

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
            lastError = "Failed to start cloudflared: " + e.getMessage();
            plugin.getLogger().log(Level.SEVERE, lastError, e);
            logManager.log("Failed to start cloudflared: " + e.getMessage());
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
                    logManager.log("Cloudflared forcefully terminated");
                }
            } catch (InterruptedException e) {
                cloudflaredProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }

            cloudflaredProcess = null;
            running.set(false);
            startTime = 0;
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
            lastError = "Max restart retries (" + maxRetries + ") reached";
            plugin.getLogger().severe(lastError);
            logManager.log("Max restart retries reached, auto-restart disabled");
            autoRestartEnabled = false;
            running.set(false);
            return;
        }

        retryCount++;
        totalRestarts++;
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

    public int getTotalRestarts() {
        return totalRestarts;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getUptime() {
        if (startTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - startTime;
    }

    public String getUptimeFormatted() {
        long uptime = getUptime();
        if (uptime == 0) {
            return "N/A";
        }

        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    public String getLastError() {
        return lastError;
    }

    public Process getProcess() {
        return cloudflaredProcess;
    }
}
