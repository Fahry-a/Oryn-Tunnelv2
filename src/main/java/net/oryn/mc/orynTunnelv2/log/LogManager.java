package net.oryn.mc.orynTunnelv2.log;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * Manages cloudflared tunnel log files with size-based rotation and archival.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Timestamped log entries</li>
 *   <li>Size-based rotation (configurable via {@code log-max-size})</li>
 *   <li>Tar+GZIP compression for archived logs</li>
 *   <li>Graceful shutdown archival on server stop</li>
 * </ul>
 */
public class LogManager {

    private final Logger logger;
    private final File logsDir;
    private final File logFile;
    private PrintWriter logWriter;
    private volatile long logMaxSize;
    private long currentSize;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ARCHIVE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public LogManager(File dataFolder, Logger logger) {
        this(dataFolder, logger, 10 * 1024 * 1024);
    }

    public LogManager(File dataFolder, Logger logger, long logMaxSize) {
        this.logger = logger;
        this.logsDir = new File(dataFolder, "logs");
        this.logFile = new File(logsDir, "log.txt");
        this.logMaxSize = logMaxSize;
        init();
    }

    private void init() {
        if (!logsDir.exists() && !logsDir.mkdirs()) {
            logger.warning("Failed to create logs directory");
        }
        try {
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
            currentSize = logFile.exists() ? logFile.length() : 0;
        } catch (IOException e) {
            logger.severe("Failed to create log file: " + e.getMessage());
        }
    }

    public synchronized void setLogMaxSize(long maxBytes) {
        this.logMaxSize = maxBytes;
    }

    public synchronized void log(String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String line = "[" + timestamp + "] " + message;
            logWriter.println(line);
            currentSize += line.getBytes().length + 1;

            if (logMaxSize > 0 && currentSize >= logMaxSize) {
                rotate();
            }
        }
    }

    /**
     * Rotate the current log file when it exceeds {@code logMaxSize}.
     * Compresses the old log to tar+gzip and starts a fresh log file.
     */
    private void rotate() {
        close();

        if (!logFile.exists() || logFile.length() == 0) {
            init();
            return;
        }

        compressAndArchive("rotate");
        init();
    }

    /**
     * Archive the current log file on server shutdown.
     * Compresses the log to tar+gzip for permanent storage.
     */
    public void archive() {
        close();

        if (!logFile.exists() || logFile.length() == 0) {
            return;
        }

        compressAndArchive("archive");
        // Do NOT re-init — server is shutting down
    }

    /**
     * Core compression logic shared by rotate() and archive().
     * Compresses the current log file to tar+gzip format.
     *
     * @param reason the reason for compression ("rotate" or "archive")
     */
    private void compressAndArchive(String reason) {
        File archiveDir = new File(logsDir, "archive");
        if (!archiveDir.exists() && !archiveDir.mkdirs()) {
            logger.warning("Failed to create archive directory");
            return;
        }

        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMAT);
        String archiveName = "cloudflared-" + timestamp + ".tar.gz";
        File archiveFile = new File(archiveDir, archiveName);

        try (FileInputStream fis = new FileInputStream(logFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(archiveFile);
             GZIPOutputStream gzipOut = new GZIPOutputStream(fos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {

            TarArchiveEntry entry = new TarArchiveEntry(logFile.getName());
            entry.setSize(logFile.length());
            tarOut.putArchiveEntry(entry);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) > 0) {
                tarOut.write(buffer, 0, len);
            }

            tarOut.closeArchiveEntry();

            if (!logFile.delete()) {
                logger.warning("Failed to delete log file after " + reason);
            }

            logger.info("Log " + reason + "d: " + archiveName);

        } catch (IOException e) {
            logger.warning("Failed to " + reason + " log: " + e.getMessage());
        }
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
    }
}
