package net.oryn.mc.orynTunnelv2.log;

import net.oryn.mc.orynTunnelv2.OrynTunnelv2;

import com.github.luben.zstd.ZstdOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {

    private final OrynTunnelv2 plugin;
    private final File logsDir;
    private final File logFile;
    private PrintWriter logWriter;

    public LogManager(OrynTunnelv2 plugin) {
        this.plugin = plugin;
        this.logsDir = new File(plugin.getDataFolder(), "logs");
        this.logFile = new File(logsDir, "log.txt");
        init();
    }

    private void init() {
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create logs directory");
        }
        try {
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create log file: " + e.getMessage());
        }
    }

    public void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            logWriter.println("[" + timestamp + "] " + message);
        }
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }

    public void archive() {
        close();

        if (!logFile.exists() || logFile.length() == 0) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        File archiveDir = new File(logsDir, "archive");
        if (!archiveDir.exists() && !archiveDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create archive directory");
            return;
        }

        String archiveName = "cloudflared-" + timestamp + ".zst";
        File archiveFile = new File(archiveDir, archiveName);

        try (FileInputStream fis = new FileInputStream(logFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(archiveFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zstdOut = new ZstdOutputStream(bos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                zstdOut.write(buffer, 0, len);
            }

            if (!logFile.delete()) {
                plugin.getLogger().warning("Failed to delete log file after archiving");
            }

            plugin.getLogger().info("Log archived: " + archiveName);

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to archive log: " + e.getMessage());
        }

        init();
    }
}
